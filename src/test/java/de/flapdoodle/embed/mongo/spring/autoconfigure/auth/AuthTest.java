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
package de.flapdoodle.embed.mongo.spring.autoconfigure.auth;

import de.flapdoodle.embed.mongo.spring.autoconfigure.customize.LocalConfig;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

@AutoConfigureDataMongo
@SpringBootTest(
	properties = {
		"de.flapdoodle.mongodb.embedded.version=4.4.18",
		"spring.data.mongodb.username=customUser",
		"spring.data.mongodb.password=userPassword123",
	}
)
@EnableAutoConfiguration
@DirtiesContext
public class AuthTest {

	@Test
	void example(@Autowired final MongoTemplate mongoTemplate) {
		assertThat(mongoTemplate.getDb()).isNotNull();

		mongoTemplate.getDb().getCollection("first").insertOne(new Document("value","a"));
		mongoTemplate.getDb().getCollection("second").insertOne(new Document("value","b"));

		Document result = mongoTemplate.getDb().runCommand(new Document("listCollections", 1));
		assertThat(result).containsEntry("ok", 1.0);
		assertThat(result)
			.extracting(doc -> doc.get("cursor"), as(MAP))
			.containsKey("firstBatch")
			.extracting(cursor -> cursor.get("firstBatch"), as(LIST))
			.hasSize(2)
			.anySatisfy(collection -> assertThat(collection)
				.isInstanceOfSatisfying(Document.class, col -> assertThat(col)
					.extracting(c -> c.get("name")).isEqualTo("first")))
			.anySatisfy(collection -> assertThat(collection)
				.isInstanceOfSatisfying(Document.class, col -> assertThat(col)
					.extracting(c -> c.get("name")).isEqualTo("second")));
	}

}
