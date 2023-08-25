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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SyncClientServerFactory extends AbstractServerFactory<MongoClient> {
	private static Logger logger = LoggerFactory.getLogger(SyncClientServerFactory.class);

	SyncClientServerFactory(MongoProperties properties) {
		super(properties);
		logger.info("sync server factory");
	}

	protected MongoClient client(ServerAddress serverAddress) {
		return MongoClients.create("mongodb://"+serverAddress);
	}

	protected MongoClient client(ServerAddress serverAddress, MongoCredential credential) {
		return MongoClients.create(MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString("mongodb://"+serverAddress))
			.credential(credential)
			.build());
	}

	protected Document resultOfAction(MongoClient client, MongoClientAction.Action action) {
		if (action instanceof MongoClientAction.RunCommand) {
			return client.getDatabase(action.database()).runCommand(((MongoClientAction.RunCommand) action).command());
		}
		throw new IllegalArgumentException("Action not supported: "+action);
	}
}
