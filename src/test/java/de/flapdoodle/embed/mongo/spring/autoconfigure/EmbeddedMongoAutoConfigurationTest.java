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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.reverse.Listener;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link EmbeddedMongoAutoConfiguration}.
 *
 * copy of spring 2.7 @{@link org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfigurationTests}
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
		String version = Version.V3_6_5.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void mongodb6() {
		String version = Version.V6_0_1.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void customUnknownVersion() {
		assertVersionConfiguration("3.6.0", "3.6.0");
	}
	
	@Test
	void useRandomPortByDefault() {
		loadWithValidVersion();
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		MongoProperties properties = this.context.getBean(MongoProperties.class);
		assertThat(getPort(client)).isEqualTo(properties.getPort());
	}

	@Test
	void specifyPortToZeroAllocateRandomPort() {
		loadWithValidVersion("spring.data.mongodb.port=0");
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		MongoProperties properties = this.context.getBean(MongoProperties.class);
		assertThat(getPort(client)).isEqualTo(properties.getPort());
		assertThat(getPort(client)).isNotEqualTo(0);
	}

	@Test
	void useSpecifiedPort() throws IOException {
		int port = Network.freeServerPort(Network.getLocalHost());
		loadWithValidVersion("spring.data.mongodb.port="+port);
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		MongoProperties properties = this.context.getBean(MongoProperties.class);
		assertThat(getPort(client)).isEqualTo(properties.getPort());
		assertThat(getPort(client)).isEqualTo(port);
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
			TestPropertyValues.of("de.flapdoodle.mongodb.embedded.version=3.6.5").applyTo(ctx);
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
		MongoClient mongoClient(/*@Value("${local.mongo.port}") int port*/MongoProperties properties) {
			return MongoClients.create("mongodb://"+properties.getHost()+":" + properties.getPort());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMongoConfiguration {

		@Bean(initMethod = "start", destroyMethod = "stop")
		MongodWrapper customMongoServer() {
			return new MongodWrapper(Mongod.instance().transitions(Version.V3_4_15), Listener.typedBuilder().build());
		}

	}

}