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
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoClientDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.data.mongo.ReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.ReactiveMongoClientFactoryBean;

import java.io.IOException;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 * 
 * copy of @{@link org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration}
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ MongoProperties.class, EmbeddedMongoProperties.class })
@AutoConfigureBefore({MongoAutoConfiguration.class, org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class })
@ConditionalOnClass({ MongoClientSettings.class, Mongod.class })
@Import({
	EmbeddedMongoAutoConfiguration.EmbeddedMongoClientDependsOnBeanFactoryPostProcessor.class,
	EmbeddedMongoAutoConfiguration.EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor.class,
})
public class EmbeddedMongoAutoConfiguration {

	@ConditionalOnClass({ com.mongodb.client.MongoClient.class, MongoClientFactoryBean.class })
	static class SyncClientServerWrapperConfig {

		@Bean(initMethod = "start", destroyMethod = "stop")
		@ConditionalOnMissingBean
		public MongodWrapper syncClientServerWrapper(
			ApplicationContext context,
			MongoProperties properties,
			EmbeddedMongoProperties embeddedProperties,
			ImmutableMongod immutableMongod,
			MongodArguments mongodArguments,
			ProcessOutput processOutput,
			ProgressListener progressListener) throws IOException {
			return new SyncClientServerFactory(properties)
				.createWrapper(context,embeddedProperties,immutableMongod,mongodArguments,processOutput,progressListener);
		}

	}


	@ConditionalOnClass({ com.mongodb.reactivestreams.client.MongoClient.class, ReactiveMongoClientFactoryBean.class })
	static class ReactiveClientServerWrapperConfig {

		@Bean(initMethod = "start", destroyMethod = "stop")
		@ConditionalOnMissingBean
		public MongodWrapper reactiveClientServerWrapper(
			ApplicationContext context,
			MongoProperties properties,
			EmbeddedMongoProperties embeddedProperties,
			ImmutableMongod immutableMongod,
			MongodArguments mongodArguments,
			ProcessOutput processOutput,
			ProgressListener progressListener) throws IOException {
			return new ReactiveClientServerFactory(properties)
				.createWrapper(context,embeddedProperties,immutableMongod,mongodArguments,processOutput,progressListener);
		}
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
	public MongodArguments mongodArguments(EmbeddedMongoProperties embeddedProperties, MongoProperties mongoProperties) {
		ImmutableMongodArguments.Builder builder = MongodArguments.builder();
		EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();

		if (storage != null && storage.getReplSetName() !=null ) {
			String replSetName = storage.getReplSetName();
			int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
			builder.replication(Storage.of(replSetName, oplogSize));
		}
		if (mongoProperties.getUsername()!=null && mongoProperties.getPassword()!=null) {
			builder.auth(true);
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
}
