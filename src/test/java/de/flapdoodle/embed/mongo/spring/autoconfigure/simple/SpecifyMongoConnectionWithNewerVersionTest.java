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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
	"de.flapdoodle.mongodb.embedded.version=6.0.3"
	,"spring.data.mongodb.uri=mongodb://localhost/test"
})
public class SpecifyMongoConnectionWithNewerVersionTest {
	@Test
	void example(@Autowired final MongoTemplate mongoTemplate) {
		assertThat(mongoTemplate.getDb()).isNotNull();
		ArrayList<String> names = mongoTemplate.getDb()
			.listCollectionNames()
			.into(new ArrayList<>());

		assertThat(names).isEmpty();
	}
}
