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
//import de.flapdoodle.embed.mongo.Command;
//import de.flapdoodle.embed.mongo.MongodWrapper;
//import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.embed.mongo.commands.ImmutableMongodArguments;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.mongo.transitions.Mongod;
//import de.flapdoodle.embed.process.config.RuntimeConfig;
//import de.flapdoodle.embed.process.config.io.ProcessOutput;
//import de.flapdoodle.embed.process.config.store.DownloadConfig;
//import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.distribution.Version.GenericVersion;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.ProgressListeners;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
//import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
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

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodWrapper embeddedMongod(ApplicationContext context, EmbeddedMongoProperties embeddedProperties,
		MongodArguments mongodArguments) throws IOException {
		IFeatureAwareVersion version = determineVersion("de.flapdoodle", embeddedProperties.getVersion());

		Integer configuredPort = this.properties.getPort();

		Net net = (configuredPort != null && configuredPort > 0)
			? Net.of(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6())
			: Net.of(getHost().getHostAddress(), Network.freeServerPort(getHost()), Network.localhostIsIPv6());

		if (configuredPort == null || configuredPort == 0) {
			setEmbeddedPort(context, net.getPort());
		}

		Logger logger = LoggerFactory.getLogger(getClass().getPackage().getName() + ".EmbeddedMongo");

		ProcessOutput processOutput = ProcessOutput.builder()
			.output(Processors.logTo(logger, Slf4jLevel.INFO))
			.error(Processors.logTo(logger, Slf4jLevel.ERROR))
			.commands(Processors.named("[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)))
			.build();

		Slf4jProgressListener progressListener = new Slf4jProgressListener(logger);

		Mongod mongod = new Mongod() {
			@Override
			public Transition<MongodArguments> mongodArguments() {
				return Start.to(MongodArguments.class).initializedWith(mongodArguments);
			}

			@Override
			public Transition<Net> net() {
				return Start.to(Net.class).initializedWith(net);
			}

			@Override
			public Transition<ProcessOutput> processOutput() {
				return Start.to(ProcessOutput.class).initializedWith(processOutput);
			}
		};

		return new MongodWrapper(mongod.transitions(version), progressListener);
	}

	@Bean
	@ConditionalOnMissingBean
	public MongodArguments mongodArguments(EmbeddedMongoProperties embeddedProperties) {
		ImmutableMongodArguments.Builder builder = MongodArguments.builder();
		EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
		if (storage != null && storage.getReplSetName() !=null ) {
			String databaseDir = storage.getDatabaseDir();
			String replSetName = storage.getReplSetName();
			int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
			builder.replication(Storage.of(replSetName, oplogSize));
		}

		MongodArguments mongodArguments = builder.build();
		return mongodArguments;
	}

//	@Bean(initMethod = "start", destroyMethod = "stop")
//	@ConditionalOnMissingBean
//	public MongodWrapper embeddedMongoServer(MongodConfig mongodConfig, RuntimeConfig runtimeConfig,
//		ApplicationContext context) {
//		Integer configuredPort = this.properties.getPort();
//		if (configuredPort == null || configuredPort == 0) {
//			setEmbeddedPort(context, mongodConfig.net().getPort());
//		}
//		MongodStarter mongodStarter = getMongodStarter(runtimeConfig);
//		return mongodStarter.prepare(mongodConfig);
//	}
//
//	private MongodStarter getMongodStarter(RuntimeConfig runtimeConfig) {
//		if (runtimeConfig == null) {
//			return MongodStarter.getDefaultInstance();
//		}
//		return MongodStarter.getInstance(runtimeConfig);
//	}
//
//	@Bean
//	@ConditionalOnMissingBean
//	public MongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {
//		ImmutableMongodConfig.Builder builder = MongodConfig.builder().version(determineVersion("de.flapdoodle", embeddedProperties.getVersion()));
//		EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
//		if (storage != null) {
//			String databaseDir = storage.getDatabaseDir();
//			String replSetName = storage.getReplSetName();
//			int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
//			builder.replication(Storage.of(replSetName, oplogSize));
//		}
//		Integer configuredPort = this.properties.getPort();
//		if (configuredPort != null && configuredPort > 0) {
//			builder.net(new Net(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6()));
//		}
//		else {
//			builder.net(new Net(getHost().getHostAddress(), Network.getFreeServerPort(getHost()),
//				Network.localhostIsIPv6()));
//		}
//		return builder.build();
//	}

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

	static class MongodWrapper {

		private final Transitions transitions;
		private final ProgressListener progressListener;
		private TransitionWalker.ReachedState<RunningMongodProcess> runningMongo=null;

		public MongodWrapper(Transitions transitions, ProgressListener progressListener) {
			this.transitions = transitions;
			this.progressListener = progressListener;
		}

		private void start() {
			try (ProgressListeners.RemoveProgressListener ignored = ProgressListeners.setProgressListener(progressListener)) {
				runningMongo = transitions.walker().initState(StateID.of(RunningMongodProcess.class));
			}
		}
		
		private void stop() {
			Preconditions.checkNotNull(runningMongo,"stop called, but runningMongo is null");
			runningMongo.close();
		}
	}
}
