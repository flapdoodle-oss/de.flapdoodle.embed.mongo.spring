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
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.reverse.transitions.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
import java.util.function.Function;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 *
 * copy of @{@link org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration}
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ MongoProperties.class, EmbeddedMongoProperties.class })
@AutoConfigureBefore({ MongoAutoConfiguration.class, org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class })
@ConditionalOnClass({ MongoClientSettings.class, Mongod.class })
@Import({
	EmbeddedMongoAutoConfiguration.EmbeddedMongoClientDependsOnBeanFactoryPostProcessor.class,
	EmbeddedMongoAutoConfiguration.EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor.class,
})
public class EmbeddedMongoAutoConfiguration {
	private static final byte[] IP4_LOOPBACK_ADDRESS = { 127, 0, 0, 1 };
	private static final byte[] IP6_LOOPBACK_ADDRESS = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

	@ConditionalOnClass({ com.mongodb.client.MongoClient.class, MongoClientFactoryBean.class })
	static class SyncClientServerWrapperConfig {

		@Bean(initMethod = "start", destroyMethod = "stop")
		@ConditionalOnMissingBean
		public MongodWrapper syncClientServerWrapper(
			IFeatureAwareVersion version,
			MongoProperties properties,
			Mongod mongod,
			MongodArguments mongodArguments) {
			return new SyncClientServerFactory(properties)
				.createWrapper(version, mongod, mongodArguments);
		}

	}

	@ConditionalOnClass({ com.mongodb.reactivestreams.client.MongoClient.class, ReactiveMongoClientFactoryBean.class })
	static class ReactiveClientServerWrapperConfig {

		@Bean(initMethod = "start", destroyMethod = "stop")
		@ConditionalOnMissingBean
		public MongodWrapper reactiveClientServerWrapper(
			IFeatureAwareVersion version,
			MongoProperties properties,
			Mongod mongod,
			MongodArguments mongodArguments) {
			return new ReactiveClientServerFactory(properties)
				.createWrapper(version, mongod, mongodArguments);
		}
	}

	@Bean
	public IFeatureAwareVersion version(EmbeddedMongoProperties embeddedProperties) {
		return determineVersion("de.flapdoodle", embeddedProperties.getVersion());
	}

	private static IFeatureAwareVersion determineVersion(String prefix, String version) {
		Assert.state(version != null, "Set the " + prefix + ".mongodb.embedded.version property or "
			+ "define your own " + IFeatureAwareVersion.class.getSimpleName() + " bean to use embedded MongoDB");
		return Versions.withFeatures(createEmbeddedMongoVersion(version));
	}

	private static Version.GenericVersion createEmbeddedMongoVersion(String version) {
		return Version.of(version);
	}

	@Bean
	public Net net(ApplicationContext context, MongoProperties properties) throws IOException {
		Integer configuredPort = properties.getPort();
		Net net = net(properties, configuredPort);

		if (configuredPort == null || configuredPort == 0) {
			setEmbeddedPort(context, net.getPort());
		}

		return net;
	}

	private static InetAddress getHost(MongoProperties properties) throws UnknownHostException {
		if (properties.getHost() == null) {
			return InetAddress.getByAddress(Network.localhostIsIPv6()
				? IP6_LOOPBACK_ADDRESS
				: IP4_LOOPBACK_ADDRESS);
		}
		return InetAddress.getByName(properties.getHost());
	}

	private static Net net(MongoProperties properties, Integer configuredPort) throws IOException {
		InetAddress host = getHost(properties);

		return (configuredPort != null && configuredPort > 0)
			? Net.of(host.getHostAddress(), configuredPort, Network.localhostIsIPv6())
			: Net.of(host.getHostAddress(), Network.freeServerPort(host), Network.localhostIsIPv6());
	}

	@Bean
	@ConditionalOnMissingBean
	public Mongod mongod(MongodArguments mongodArguments, ProcessOutput processOutput, Net net,
		ProgressListener progressListener) {

		ImmutableMongod copy = Mongod.builder()
			.mongodArguments(Start.to(MongodArguments.class).initializedWith(mongodArguments))
			.net(Start.to(Net.class).initializedWith(net))
			.processOutput(Start.to(ProcessOutput.class).initializedWith(processOutput))
			.build();

		if (progressListener != null) {
			copy = copy
				.withProgressListener(Start.to(ProgressListener.class).initializedWith(progressListener));
		}

		return copy;
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
	public MongodArguments mongodArguments() {
		return MongodArguments.defaults();
	}

	@Bean
	public BeanPostProcessor fixTransactionAndAuth(EmbeddedMongoProperties embeddedProperties, MongoProperties mongoProperties) {
		EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();

		return new TypedBeanPostProcessor<>(MongodArguments.class, src -> {
			ImmutableMongodArguments.Builder builder = MongodArguments.builder()
				.from(src);

			if (storage != null && storage.getReplSetName() != null && !src.replication().isPresent()) {
				String replSetName = storage.getReplSetName();
				int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;

				builder
					.replication(Storage.of(replSetName, oplogSize))
					.useNoJournal(false);
			} else {
				if (src.replication().isPresent()) {
					builder.useNoJournal(false);
				}
			}

			if (mongoProperties.getUsername() != null && mongoProperties.getPassword() != null) {
				builder.auth(true);
			}

			return builder.build();
		}, Function.identity());
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
