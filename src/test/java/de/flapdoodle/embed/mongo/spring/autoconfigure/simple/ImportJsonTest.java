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
package de.flapdoodle.embed.mongo.spring.autoconfigure.simple;

import de.flapdoodle.embed.mongo.commands.MongoImportArguments;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
@Import(ImportJsonTest.Config.class)
public class ImportJsonTest {
	@Test
	void example(@Autowired final MongoTemplate mongoTemplate) {
		assertThat(mongoTemplate.getDb()).isNotNull();

		ArrayList<Document> first = mongoTemplate.getDb()
			.getCollection("first")
			.find()
			.into(new ArrayList<>());

		assertThat(first).hasSize(3)
			.anyMatch(doc -> doc.get("name", String.class).equals("Cassandra"));

		ArrayList<Document> second = mongoTemplate.getDb()
			.getCollection("second")
			.find()
			.into(new ArrayList<>());

		assertThat(second).hasSize(2)
			.anyMatch(doc -> doc.get("name", String.class).equals("Susi"));
	}

	static class Config {
		@Bean
		public List<MongoImportArguments> jsonImportArguments() {
			return Arrays.asList(MongoImportArguments.builder()
					.databaseName("test")
					.collectionName("first")
					.importFile(ImportJsonTest.class.getResource("/first.json").getFile())
					.isJsonArray(true)
					.upsertDocuments(true)
					.build(),
				MongoImportArguments.builder()
					.databaseName("test")
					.collectionName("second")
					.importFile(ImportJsonTest.class.getResource("/second.json").getFile())
					.isJsonArray(true)
					.upsertDocuments(true)
					.build());
		}
	}
}
