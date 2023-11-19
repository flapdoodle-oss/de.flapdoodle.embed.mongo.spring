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

import de.flapdoodle.embed.mongo.client.AuthenticationSetup;
import de.flapdoodle.embed.mongo.client.ClientActions;
import de.flapdoodle.embed.mongo.client.ExecuteMongoClientAction;
import de.flapdoodle.embed.mongo.client.UsernamePassword;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.reverse.Listener;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;

import java.io.Closeable;
import java.util.*;

public abstract class AbstractServerFactory<C extends Closeable> {

	private final MongoProperties properties;
	private final ExecuteMongoClientAction<C> adapter;

	protected AbstractServerFactory(MongoProperties properties, ExecuteMongoClientAction<C> adapter) {
		this.properties = properties;
		this.adapter = adapter;
	}

	public final MongodWrapper createWrapper(
		IFeatureAwareVersion version,
		Mongod mongod,
		MongodArguments mongodArguments
	) {
		return new MongodWrapper(
			mongod.transitions(version),
			addAuthUserToDB(properties),
			initReplicaSet(version, properties, mongodArguments)
		);
	}

	private Listener addAuthUserToDB(MongoProperties properties) {
		String username = properties.getUsername();
		char[] password = properties.getPassword();
		String databaseName = properties.getMongoClientDatabase();

		if (username != null && password != null) {
			return ClientActions.setupAuthentication(adapter,
				databaseName,
				AuthenticationSetup.of(UsernamePassword.of(username, password))
			);
		} else {
			return Listener.builder().build();
		}
	}

	private Listener initReplicaSet(IFeatureAwareVersion version, MongoProperties properties, MongodArguments mongodArguments) {
		String username = properties.getUsername();
		char[] password = properties.getPassword();
		Optional<Storage> replication = mongodArguments.replication();

		if (replication.isPresent()) {
			return ClientActions.initReplicaSet(adapter, version, replication.get(), username != null ?
				Optional.of(UsernamePassword.of(username, password)) : Optional.empty());
		} else {
			return Listener.builder().build();
		}
	}
}
