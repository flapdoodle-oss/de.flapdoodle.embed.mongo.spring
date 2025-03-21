/*
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
package de.flapdoodle.embed.mongo.spring.autoconfigure.base;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import de.flapdoodle.embed.mongo.spring.autoconfigure.MongodWrapper;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.reverse.Listener;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link EmbeddedMongoAutoConfiguration}.
 *
 * copy of @{@link org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfigurationTests}
 */
class EmbeddedMongoAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void noVersion() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.port=0").applyTo(this.context);
		this.context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
			EmbeddedMongoAutoConfiguration.class);
		assertThatThrownBy(() -> this.context.refresh()).hasRootCauseExactlyInstanceOf(IllegalStateException.class)
			.hasRootCauseMessage("Set the de.flapdoodle.mongodb.embedded.version property or define your own IFeatureAwareVersion "
				+ "bean to use embedded MongoDB");
	}

	@Test
	void customVersion() {
		String version = Version.V4_4_5.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void mongodb6() {
		String version = Version.V6_0_12.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void mongodb7() {
		String version = Version.V7_0_4.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void mongodb8() {
		String version = Version.V8_0_3.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void customUnknownVersion() {
		assertVersionConfiguration("4.4.0", "4.4.0");
	}
	
	@Test
	void useRandomPortByDefault() {
		loadWithValidVersion();
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(getPort(client)).isEqualTo(mongoPort);
	}

	@Test
	void specifyPortToZeroAllocateRandomPort() {
		loadWithValidVersion("spring.data.mongodb.port=0");
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(getPort(client)).isEqualTo(mongoPort);
	}

	@Test
	void randomlyAllocatedPortIsAvailableWhenCreatingMongoClient() {
		loadWithValidVersion(MongoClientConfiguration.class);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(getPort(client)).isEqualTo(mongoPort);
	}

	@Test
	void portIsAvailableInParentContext() {
		try (ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext()) {
			TestPropertyValues.of("de.flapdoodle.mongodb.embedded.version=3.5.5").applyTo(parent);
			parent.refresh();
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			this.context.register(EmbeddedMongoAutoConfiguration.class, MongoClientConfiguration.class, MongoAutoConfiguration.class);
			this.context.refresh();
			assertThat(parent.getEnvironment().getProperty("local.mongo.port")).isNotNull();
		}
	}

	@Test
	void defaultStorageConfiguration() {
		loadWithValidVersion(MongoClientConfiguration.class);
		Optional<Storage> replication = this.context.getBean(MongodArguments.class).replication();
		assertThat(replication.isPresent()).isFalse();
	}

	@Test
	void customOpLogSizeIsAppliedToConfiguration() {
		loadWithValidVersion(
			"de.flapdoodle.mongodb.embedded.storage.replSetName=testing",
			"de.flapdoodle.mongodb.embedded.storage.oplogSize=1024KB"
		);
		Optional<Storage> replication = this.context.getBean(MongodArguments.class).replication();
		assertThat(replication.get().getOplogSize()).isEqualTo(1);
	}

	@Test
	void customOpLogSizeUsesMegabytesPerDefault() {
		loadWithValidVersion(
			"de.flapdoodle.mongodb.embedded.storage.replSetName=testing",
			"de.flapdoodle.mongodb.embedded.storage.oplogSize=10"
		);
		Optional<Storage> replication = this.context.getBean(MongodArguments.class).replication();
		assertThat(replication.get().getOplogSize()).isEqualTo(10);
	}

	@Test
	void customReplicaSetNameIsAppliedToConfiguration() {
		loadWithValidVersion("de.flapdoodle.mongodb.embedded.storage.replSetName=testing");
		Optional<Storage> replication = this.context.getBean(MongodArguments.class).replication();
		assertThat(replication.get().getReplSetName()).isEqualTo("testing");
	}

	@Test
	void customMongoServerConfiguration() {
		loadWithValidVersion(CustomMongoConfiguration.class);
		Map<String, MongoClient> mongoClients = this.context.getBeansOfType(MongoClient.class);
		assertThat(mongoClients).isNotEmpty();
		for (String mongoClientBeanName : mongoClients.keySet()) {
			BeanDefinition beanDefinition = this.context.getBeanFactory().getBeanDefinition(mongoClientBeanName);
			assertThat(beanDefinition.getDependsOn()).contains("customMongoServer");
		}
	}

	@Test
	void withoutAuth() {
		loadWithValidVersion();

		try(MongoClient client = this.context.getBean(MongoClient.class)) {
			ArrayList<String> collectionNames = client.getDatabase("test")
				.listCollectionNames()
				.into(new ArrayList<>());

			assertThat(collectionNames).isEmpty();
		}
	}

	@Test
	void withAuth() {
		loadWithValidVersion(
			"spring.data.mongodb.username=user",
			"spring.data.mongodb.password=passwd");

		try(MongoClient client = this.context.getBean(MongoClient.class)) {
			ArrayList<String> collectionNames = client.getDatabase("test")
				.listCollectionNames()
				.into(new ArrayList<>());
			
			assertThat(collectionNames).isEmpty();
		}
	}

	@Test
	void mongoDb6withAuth() {
		loadWithValidVersion(
			"spring.data.mongodb.username=user",
			"spring.data.mongodb.password=passwd",
			"de.flapdoodle.mongodb.embedded.version=6.0.1");

		try(MongoClient client = this.context.getBean(MongoClient.class)) {
			ArrayList<String> collectionNames = client.getDatabase("test")
				.listCollectionNames()
				.into(new ArrayList<>());

			assertThat(collectionNames).isEmpty();
		}
	}

	private void assertVersionConfiguration(String configuredVersion, String expectedVersion) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.port=0").applyTo(this.context);
		if (configuredVersion != null) {
			TestPropertyValues.of("de.flapdoodle.mongodb.embedded.version=" + configuredVersion).applyTo(this.context);
		}
		this.context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
			EmbeddedMongoAutoConfiguration.class);
		this.context.refresh();
		MongoTemplate mongo = this.context.getBean(MongoTemplate.class);
		Document buildInfo = mongo.executeCommand("{ buildInfo: 1 }");

		assertThat(buildInfo.getString("version")).isEqualTo(expectedVersion);
	}

	private void loadWithValidVersion(String... environment) {
		loadWithValidVersion(null, environment);
	}

	private void loadWithValidVersion(Class<?> config, String... environment) {
		this.context = applicationContext(config, environment);
	}
	
	static AnnotationConfigApplicationContext applicationContext(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		if (!Arrays.asList(environment).stream().anyMatch(it -> it.startsWith("de.flapdoodle.mongodb.embedded.version"))) {
			TestPropertyValues.of("de.flapdoodle.mongodb.embedded.version=4.4.0").applyTo(ctx);
		}
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		return ctx;
	}

	private int getPort(MongoClient client) {
		return client.getClusterDescription().getClusterSettings().getHosts().get(0).getPort();
	}

	@Configuration(proxyBeanMethods = false)
	static class MongoClientConfiguration {

		@Bean
		MongoClient mongoClient(@Value("${local.mongo.port}") int port) {
			return MongoClients.create("mongodb://localhost:" + port);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMongoConfiguration {

		@Bean(initMethod = "start", destroyMethod = "stop")
		MongodWrapper customMongoServer() {
			return new MongodWrapper(Mongod.instance().transitions(Version.V4_4_5), Listener.typedBuilder().build());
		}

	}

}