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
package de.flapdoodle.embed.mongo.spring.autoconfigure.mongoclientsettings;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureDataMongo
@SpringBootTest()
@EnableAutoConfiguration
@DirtiesContext
public class CustomizeMongoClientSettingsTest {

	@Test
	void example(@Autowired final MongoTemplate mongoTemplate) {
		LocalDateTime dateTime = LocalDateTime.of(2027, Month.AUGUST, 3, 12, 0, 5);

		mongoTemplate.getDb().getCollection("sample")
			.insertOne(
				new Document("name", "klaus")
					.append("meetMe", dateTime)
			);

		Document first = mongoTemplate.getDb().getCollection("sample").find().first();
		assertThat(first).isNotNull();
		assertThat(first.get("meetMe")).isEqualTo(dateTime);
	}

}
