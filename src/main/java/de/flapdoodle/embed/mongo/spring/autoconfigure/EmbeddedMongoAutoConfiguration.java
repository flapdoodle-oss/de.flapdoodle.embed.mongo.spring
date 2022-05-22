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
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig;
import de.flapdoodle.embed.process.distribution.Version.GenericVersion;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.stream.Stream;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 * 
 * copy of @{@link org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration}
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ MongoProperties.class, EmbeddedMongoProperties.class, LegacyEmbeddedMongoProperties.class })
@AutoConfigureBefore({MongoAutoConfiguration.class, org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class })
@ConditionalOnClass({ MongoClientSettings.class, MongodStarter.class })
@Import({ EmbeddedMongoAutoConfiguration.EmbeddedMongoClientDependsOnBeanFactoryPostProcessor.class,
	EmbeddedMongoAutoConfiguration.EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor.class })
public class EmbeddedMongoAutoConfiguration {

	private static final byte[] IP4_LOOPBACK_ADDRESS = { 127, 0, 0, 1 };

	private static final byte[] IP6_LOOPBACK_ADDRESS = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

	private final MongoProperties properties;

	public EmbeddedMongoAutoConfiguration(MongoProperties properties) {
		this.properties = properties;
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodExecutable embeddedMongoServer(MongodConfig mongodConfig, RuntimeConfig runtimeConfig,
		ApplicationContext context) {
		Integer configuredPort = this.properties.getPort();
		if (configuredPort == null || configuredPort == 0) {
			setEmbeddedPort(context, mongodConfig.net().getPort());
		}
		MongodStarter mongodStarter = getMongodStarter(runtimeConfig);
		return mongodStarter.prepare(mongodConfig);
	}

	private MongodStarter getMongodStarter(RuntimeConfig runtimeConfig) {
		if (runtimeConfig == null) {
			return MongodStarter.getDefaultInstance();
		}
		return MongodStarter.getInstance(runtimeConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	public MongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {
		ImmutableMongodConfig.Builder builder = MongodConfig.builder().version(determineVersion("de.flapdoodle", embeddedProperties.getVersion()));
		EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
		if (storage != null) {
			String databaseDir = storage.getDatabaseDir();
			String replSetName = storage.getReplSetName();
			int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
			builder.replication(new Storage(databaseDir, replSetName, oplogSize));
		}
		Integer configuredPort = this.properties.getPort();
		if (configuredPort != null && configuredPort > 0) {
			builder.net(new Net(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6()));
		}
		else {
			builder.net(new Net(getHost().getHostAddress(), Network.getFreeServerPort(getHost()),
				Network.localhostIsIPv6()));
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public MongodConfig embeddedLegacyMongoConfiguration(LegacyEmbeddedMongoProperties embeddedProperties) throws IOException {
		ImmutableMongodConfig.Builder builder = MongodConfig.builder().version(determineVersion("spring", embeddedProperties.getVersion()));
		LegacyEmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
		if (storage != null) {
			String databaseDir = storage.getDatabaseDir();
			String replSetName = storage.getReplSetName();
			int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
			builder.replication(new Storage(databaseDir, replSetName, oplogSize));
		}
		Integer configuredPort = this.properties.getPort();
		if (configuredPort != null && configuredPort > 0) {
			builder.net(new Net(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6()));
		}
		else {
			builder.net(new Net(getHost().getHostAddress(), Network.getFreeServerPort(getHost()),
				Network.localhostIsIPv6()));
		}
		return builder.build();
	}

	private IFeatureAwareVersion determineVersion(String prefix, String version) {
		Assert.state(version != null, "Set the "+prefix+".mongodb.embedded.version property or "
			+ "define your own MongodConfig bean to use embedded MongoDB");
		return Versions.withFeatures(createEmbeddedMongoVersion(version));
	}

	private GenericVersion createEmbeddedMongoVersion(String version) {
		return de.flapdoodle.embed.process.distribution.Version.of(version);
	}

	private InetAddress getHost() throws UnknownHostException {
		if (this.properties.getHost() == null) {
			return InetAddress.getByAddress(Network.localhostIsIPv6() ? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
		}
		return InetAddress.getByName(this.properties.getHost());
	}

	private void setEmbeddedPort(ApplicationContext context, int port) {
		setPortProperty(context, port);
	}

	private void setPortProperty(ApplicationContext currentContext, int port) {
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
	private Map<String, Object> getMongoPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("mongo.ports");
		if (propertySource == null) {
			propertySource = new MapPropertySource("mongo.ports", new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Logger.class)
	@ConditionalOnMissingBean(RuntimeConfig.class)
	static class RuntimeConfigConfiguration {

		@Bean
		RuntimeConfig embeddedMongoRuntimeConfig(
			ObjectProvider<DownloadConfigBuilderCustomizer> downloadConfigBuilderCustomizers) {
			Logger logger = LoggerFactory.getLogger(getClass().getPackage().getName() + ".EmbeddedMongo");
			ProcessOutput processOutput = new ProcessOutput(Processors.logTo(logger, Slf4jLevel.INFO),
				Processors.logTo(logger, Slf4jLevel.ERROR),
				Processors.named("[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)));
			return Defaults.runtimeConfigFor(Command.MongoD, logger).processOutput(processOutput)
				.artifactStore(getArtifactStore(logger, downloadConfigBuilderCustomizers.orderedStream()))
				.isDaemonProcess(false).build();
		}

		private ExtractedArtifactStore getArtifactStore(Logger logger,
			Stream<DownloadConfigBuilderCustomizer> downloadConfigBuilderCustomizers) {
			ImmutableDownloadConfig.Builder downloadConfigBuilder = Defaults.downloadConfigFor(Command.MongoD);
			downloadConfigBuilder.progressListener(new Slf4jProgressListener(logger));
			downloadConfigBuilderCustomizers.forEach((customizer) -> customizer.customize(downloadConfigBuilder));
			DownloadConfig downloadConfig = downloadConfigBuilder.build();
			return Defaults.extractedArtifactStoreFor(Command.MongoD).withDownloadConfig(downloadConfig);
		}

	}

	/**
	 * Post processor to ensure that {@link com.mongodb.client.MongoClient} beans depend
	 * on any {@link MongodExecutable} beans.
	 */
	@ConditionalOnClass({ com.mongodb.client.MongoClient.class, MongoClientFactoryBean.class })
	static class EmbeddedMongoClientDependsOnBeanFactoryPostProcessor
		extends MongoClientDependsOnBeanFactoryPostProcessor {

		EmbeddedMongoClientDependsOnBeanFactoryPostProcessor() {
			super(MongodExecutable.class);
		}

	}

	/**
	 * Post processor to ensure that
	 * {@link com.mongodb.reactivestreams.client.MongoClient} beans depend on any
	 * {@link MongodExecutable} beans.
	 */
	@ConditionalOnClass({ com.mongodb.reactivestreams.client.MongoClient.class, ReactiveMongoClientFactoryBean.class })
	static class EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor
		extends ReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor {

		EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor() {
			super(MongodExecutable.class);
		}

	}

}
