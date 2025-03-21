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

import com.mongodb.MongoClientSettings;
import org.bson.BsonType;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MongoClientSettingsConfig {

	@Bean
	public MongoClientSettingsBuilderCustomizer mongoClientSettingsBuilderCustomizer() {
		Map<BsonType, Class<?>> replacements = new HashMap<>();
		replacements.put(BsonType.DATE_TIME, LocalDateTime.class);
		BsonTypeClassMap classMap = new BsonTypeClassMap(replacements);

		return builder -> builder.codecRegistry(CodecRegistries.fromRegistries(
			CodecRegistries.fromProviders(new DocumentCodecProvider(classMap)),
			MongoClientSettings.getDefaultCodecRegistry(),
			CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
		));
	}
}
