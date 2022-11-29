/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	...
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.embed.mongo.spring.autoconfigure;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.transitions.Start;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractServerFactory {
	static final byte[] IP4_LOOPBACK_ADDRESS = { 127, 0, 0, 1 };

	static final byte[] IP6_LOOPBACK_ADDRESS = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

	protected final MongoProperties properties;

	protected AbstractServerFactory(MongoProperties properties) {
		this.properties = properties;
	}

	protected final InetAddress getHost() throws UnknownHostException {
		if (this.properties.getHost() == null) {
			return InetAddress.getByAddress(Network.localhostIsIPv6() ? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
		}
		return InetAddress.getByName(this.properties.getHost());
	}

	protected Net net(Integer configuredPort) throws IOException {
		return (configuredPort != null && configuredPort > 0)
			? Net.of(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6())
			: Net.of(getHost().getHostAddress(), Network.freeServerPort(getHost()), Network.localhostIsIPv6());
	}

	protected final Listener addAuthUserToDB(MongoProperties properties) {
		Listener.TypedListener.Builder typedBuilder = Listener.typedBuilder();
		String username = properties.getUsername();
		char[] password = properties.getPassword();
		String databaseName = properties.getMongoClientDatabase();

		if (username !=null && password !=null) {
			typedBuilder.onStateReached(StateID.of(RunningMongodProcess.class), addAuthUserToDBCallback(username, password, databaseName));
			typedBuilder.onStateTearDown(StateID.of(RunningMongodProcess.class), sendShutdown(username, password, databaseName));
		}
		return typedBuilder.build();
	}

	protected abstract Consumer<RunningMongodProcess> addAuthUserToDBCallback(String username, char[] password, String databaseName);

	protected abstract Consumer<RunningMongodProcess> sendShutdown(String username, char[] password, String databaseName);

	protected static ImmutableMongod mongod(ImmutableMongod immutableMongod, MongodArguments mongodArguments, ProcessOutput processOutput, Net net,
		ProgressListener progressListener) {
		ImmutableMongod copy = immutableMongod
			.withMongodArguments(Start.to(MongodArguments.class).initializedWith(mongodArguments))
			.withNet(Start.to(Net.class).initializedWith(net))
			.withProcessOutput(Start.to(ProcessOutput.class).initializedWith(processOutput));

		if (progressListener!=null) {
			copy = copy
				.withProgressListener(Start.to(ProgressListener.class).initializedWith(progressListener));
		}

		return copy;
	}

	protected static IFeatureAwareVersion determineVersion(String prefix, String version) {
		Assert.state(version != null, "Set the "+prefix+".mongodb.embedded.version property or "
			+ "define your own MongodConfig bean to use embedded MongoDB");
		return Versions.withFeatures(createEmbeddedMongoVersion(version));
	}

	protected static Version.GenericVersion createEmbeddedMongoVersion(String version) {
		return de.flapdoodle.embed.process.distribution.Version.of(version);
	}

	protected static void setEmbeddedPort(ApplicationContext context, int port) {
		setPortProperty(context, port);
	}

	protected static void setPortProperty(ApplicationContext currentContext, int port) {
		if (currentContext instanceof ConfigurableApplicationContext) {
			MutablePropertySources sources = ((ConfigurableApplicationContext) currentContext).getEnvironment()
				.getPropertySources();
			getMongoPorts(sources).put("local.mongo.port", port);
		}
		if (currentContext.getParent() != null) {
			setPortProperty(currentContext.getParent(), port);
		}
	}

	@SuppressWarnings("unchecked")
	protected static Map<String, Object> getMongoPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("mongo.ports");
		if (propertySource == null) {
			propertySource = new MapPropertySource("mongo.ports", new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

}
