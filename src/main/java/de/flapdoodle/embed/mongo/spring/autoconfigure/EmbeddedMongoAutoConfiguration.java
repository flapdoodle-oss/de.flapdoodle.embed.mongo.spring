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

import com.mongodb.MongoClientSettings;
import de.flapdoodle.embed.mongo.commands.ImmutableMongodArguments;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.process.distribution.Version.GenericVersion;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.transitions.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoClientDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.data.mongo.ReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.ReactiveMongoClientFactoryBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 * 
 * copy of @{@link org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration}
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ MongoProperties.class, EmbeddedMongoProperties.class })
@AutoConfigureBefore({MongoAutoConfiguration.class, org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class })
@ConditionalOnClass({ MongoClientSettings.class, Mongod.class })
@Import({ EmbeddedMongoAutoConfiguration.EmbeddedMongoClientDependsOnBeanFactoryPostProcessor.class,
	EmbeddedMongoAutoConfiguration.EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor.class })
public class EmbeddedMongoAutoConfiguration {

	private static final byte[] IP4_LOOPBACK_ADDRESS = { 127, 0, 0, 1 };

	private static final byte[] IP6_LOOPBACK_ADDRESS = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

	private final MongoProperties properties;

	public EmbeddedMongoAutoConfiguration(MongoProperties properties) {
		this.properties = properties;
	}

	private InetAddress getHost() throws UnknownHostException {
		if (this.properties.getHost() == null) {
			return InetAddress.getByAddress(Network.localhostIsIPv6() ? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
		}
		return InetAddress.getByName(this.properties.getHost());
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodWrapper embeddedMongod(
		ApplicationContext context, EmbeddedMongoProperties embeddedProperties,
		ImmutableMongod immutableMongod,
		MongodArguments mongodArguments,
		ProcessOutput processOutput,
		ProgressListener progressListener
	) throws IOException {
		IFeatureAwareVersion version = determineVersion("de.flapdoodle", embeddedProperties.getVersion());

		Integer configuredPort = this.properties.getPort();

		Net net = (configuredPort != null && configuredPort > 0)
			? Net.of(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6())
			: Net.of(getHost().getHostAddress(), Network.freeServerPort(getHost()), Network.localhostIsIPv6());

		if (configuredPort == null || configuredPort == 0) {
			setEmbeddedPort(context, net.getPort());
		}

		Mongod mongod = immutableMongod
			.withMongodArguments(Start.to(MongodArguments.class).initializedWith(mongodArguments))
			.withNet(Start.to(Net.class).initializedWith(net))
			.withProcessOutput(Start.to(ProcessOutput.class).initializedWith(processOutput));

		return new MongodWrapper(mongod.transitions(version), progressListener);
	}

	@Bean
	@ConditionalOnMissingBean
	public ImmutableMongod mongod() {
		return ImmutableMongod.instance();
	}

	@Bean
	@ConditionalOnMissingBean
	public ProgressListener progressListener() {
		return new Slf4jProgressListener(logger());
	}

	@Bean
	@ConditionalOnMissingBean
	public ProcessOutput processOutput() {
		Logger logger = logger();

		return ProcessOutput.builder()
			.output(Processors.logTo(logger, Slf4jLevel.INFO))
			.error(Processors.logTo(logger, Slf4jLevel.ERROR))
			.commands(Processors.named("[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)))
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public MongodArguments mongodArguments(EmbeddedMongoProperties embeddedProperties) {
		ImmutableMongodArguments.Builder builder = MongodArguments.builder();
		EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();

		if (storage != null && storage.getReplSetName() !=null ) {
			String replSetName = storage.getReplSetName();
			int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
			builder.replication(Storage.of(replSetName, oplogSize));
		}

		return builder.build();
	}

	/**
	 * Post processor to ensure that {@link com.mongodb.client.MongoClient} beans depend
	 * on any {@link MongodWrapper} beans.
	 */
	@ConditionalOnClass({ com.mongodb.client.MongoClient.class, MongoClientFactoryBean.class })
	static class EmbeddedMongoClientDependsOnBeanFactoryPostProcessor
		extends MongoClientDependsOnBeanFactoryPostProcessor {

		EmbeddedMongoClientDependsOnBeanFactoryPostProcessor() {
			super(MongodWrapper.class);
		}

	}

	/**
	 * Post processor to ensure that
	 * {@link com.mongodb.reactivestreams.client.MongoClient} beans depend on any
	 * {@link MongodWrapper} beans.
	 */
	@ConditionalOnClass({ com.mongodb.reactivestreams.client.MongoClient.class, ReactiveMongoClientFactoryBean.class })
	static class EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor
		extends ReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor {

		EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor() {
			super(MongodWrapper.class);
		}
	}


	private static Logger logger() {
		return LoggerFactory.getLogger(EmbeddedMongoAutoConfiguration.class.getPackage().getName() + ".EmbeddedMongo");
	}

	private static IFeatureAwareVersion determineVersion(String prefix, String version) {
		Assert.state(version != null, "Set the "+prefix+".mongodb.embedded.version property or "
			+ "define your own MongodConfig bean to use embedded MongoDB");
		return Versions.withFeatures(createEmbeddedMongoVersion(version));
	}

	private static GenericVersion createEmbeddedMongoVersion(String version) {
		return de.flapdoodle.embed.process.distribution.Version.of(version);
	}

	private static void setEmbeddedPort(ApplicationContext context, int port) {
		setPortProperty(context, port);
	}

	private static void setPortProperty(ApplicationContext currentContext, int port) {
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
	private static Map<String, Object> getMongoPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("mongo.ports");
		if (propertySource == null) {
			propertySource = new MapPropertySource("mongo.ports", new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

}
