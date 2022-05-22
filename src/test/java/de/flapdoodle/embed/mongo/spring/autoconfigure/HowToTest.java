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

import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

public class HowToTest {
	
	@RegisterExtension
	public static Recording recording= Recorder.with("HowTo.md", TabSize.spaces(2))
		.sourceCodeOf("autoConfigClass", AutoConfigTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("firstIsolation", AutoConfigFirstIsolationTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim)
		.sourceCodeOf("secondIsolation", AutoConfigSecondIsolationTest.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim);

	@Test
	public void noop() {
		recording.begin();
		String prefix = EmbeddedMongoProperties.class.getAnnotation(ConfigurationProperties.class).prefix();
		String legacyPrefix = LegacyEmbeddedMongoProperties.class.getAnnotation(ConfigurationProperties.class).prefix();
		recording.output("prefix", prefix);
		recording.output("legacyPrefix", legacyPrefix);
		recording.end();
	}
}
