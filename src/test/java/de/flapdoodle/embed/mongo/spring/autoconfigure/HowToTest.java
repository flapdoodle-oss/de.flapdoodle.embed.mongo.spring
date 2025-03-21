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
package de.flapdoodle.embed.mongo.spring.autoconfigure;

import de.flapdoodle.embed.mongo.spring.autoconfigure.customize.CustomizeMongodTest;
import de.flapdoodle.embed.mongo.spring.autoconfigure.customize.LocalConfig;
import de.flapdoodle.embed.mongo.spring.autoconfigure.mongoclientsettings.CustomizeMongoClientSettingsTest;
import de.flapdoodle.embed.mongo.spring.autoconfigure.mongoclientsettings.MongoClientSettingsConfig;
import de.flapdoodle.embed.mongo.spring.autoconfigure.simple.*;
import de.flapdoodle.embed.mongo.spring.autoconfigure.transactions.PersonRepository;
import de.flapdoodle.embed.mongo.spring.autoconfigure.transactions.PersonService;
import de.flapdoodle.embed.mongo.spring.autoconfigure.transactions.TransactionalConfig;
import de.flapdoodle.embed.mongo.spring.autoconfigure.transactions.TransactionalTest;
import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;

public class HowToTest {
	
	@RegisterExtension
	public static Recording recording= Recorder.with("HowTo.md", TabSize.spaces(2))
		.sourceCodeOf("autoConfigClass", AutoConfigTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("importJsonClass", ImportJsonTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("firstIsolation", AutoConfigFirstIsolationTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("secondIsolation", AutoConfigSecondIsolationTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("transaction.test", TransactionalTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("transaction.config", TransactionalConfig.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("transaction.service", PersonService.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("transaction.repository", PersonRepository.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("customDatabaseDir", CustomDatabaseDirTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("customStartTimeout", CustomStartTimeoutTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("customizeMongod", CustomizeMongodTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("customizeMongod.config", LocalConfig.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("customizeMongoClientSettings", CustomizeMongoClientSettingsTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("customizeMongoClientSettings.config", MongoClientSettingsConfig.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		;

	@Test
	public void noop() {
		recording.begin();
		String prefix = EmbeddedMongoProperties.class.getAnnotation(ConfigurationProperties.class).prefix();
		recording.output("prefix", prefix);
		recording.end();
	}
}
