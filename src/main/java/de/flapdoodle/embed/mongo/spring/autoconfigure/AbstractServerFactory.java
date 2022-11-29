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

import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;

import java.util.function.Consumer;

public abstract class AbstractServerFactory {

	protected final MongoProperties properties;
	protected AbstractServerFactory(MongoProperties properties) {
		this.properties = properties;
	}

	protected final Listener addAuthUserToDB(MongoProperties properties) {
		Listener.TypedListener.Builder typedBuilder = Listener.typedBuilder();
		String username = properties.getUsername();
		char[] password = properties.getPassword();
		String databaseName = properties.getMongoClientDatabase();

		if (username !=null && password !=null) {
			typedBuilder.onStateReached(StateID.of(RunningMongodProcess.class), addAuthUserToDBCallback(username, password, databaseName));
			typedBuilder.onStateTearDown(StateID.of(RunningMongodProcess.class), sendShutdown(username, password, databaseName));
		}
		return typedBuilder.build();
	}

	protected abstract Consumer<RunningMongodProcess> addAuthUserToDBCallback(String username, char[] password, String databaseName);

	protected abstract Consumer<RunningMongodProcess> sendShutdown(String username, char[] password, String databaseName);
}
