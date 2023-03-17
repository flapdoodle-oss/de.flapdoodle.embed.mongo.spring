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
package de.flapdoodle.embed.mongo.spring.autoconfigure.customize;

import de.flapdoodle.embed.mongo.spring.autoconfigure.TypedBeanPostProcessor;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.reverse.transitions.Start;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalConfig {

	@Bean
	BeanPostProcessor customizeMongod() {
		return TypedBeanPostProcessor.applyBeforeInitialization(Mongod.class, src -> {
			return Mongod.builder()
				.from(src)
				.processOutput(Start.to(ProcessOutput.class)
					.initializedWith(ProcessOutput.namedConsole("custom")))
				.build();
		});
	}
}
