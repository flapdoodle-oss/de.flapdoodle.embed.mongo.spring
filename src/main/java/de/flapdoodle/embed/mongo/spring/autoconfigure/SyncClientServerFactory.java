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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class SyncClientServerFactory extends AbstractServerFactory {
	private static Logger logger = LoggerFactory.getLogger(SyncClientServerFactory.class);

	SyncClientServerFactory(MongoProperties properties) {
		super(properties);
		logger.info("sync server factory");
	}

	MongodWrapper createWrapper(
		IFeatureAwareVersion version,
		Mongod mongod,
		MongodArguments mongodArguments
	) {
		return new MongodWrapper(
			mongod.transitions(version),
			addAuthUserToDB(properties),
			initReplicaSet(version, mongodArguments)
		);
	}

	private static MongoClient client(ServerAddress serverAddress) {
		return MongoClients.create("mongodb://"+serverAddress);
	}

	private static MongoClient client(ServerAddress serverAddress, MongoCredential credential) {
		return MongoClients.create(MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString("mongodb://"+serverAddress))
			.credential(credential)
			.build());
	}

	private static Listener initReplicaSet(IFeatureAwareVersion version, MongodArguments mongodArguments) {
		Listener.TypedListener.Builder builder = Listener.typedBuilder();

		if (mongodArguments.replication().isPresent() && version.enabled(Feature.RS_INITIATE)) {
			builder.onStateReached(StateID.of(RunningMongodProcess.class), runningMongodProcess -> {
				try (MongoClient client = client(runningMongodProcess.getServerAddress())) {
					client.getDatabase("admin")
						.runCommand(Document.parse("{replSetInitiate: {}}"));
				}
			});
		}

		return builder.build();
	}

	@Override
	protected Consumer<RunningMongodProcess> addAuthUserToDBCallback(String username, char[] password, String databaseName) {
		return runningMongodProcess -> {
			logger.info("enable "+username+" access for "+databaseName);

			String adminDatabaseName = "admin";

			try (MongoClient client = client(runningMongodProcess.getServerAddress())) {
				if (!createUser(client.getDatabase(adminDatabaseName), username, password, "root")) {
					throw new IllegalArgumentException("could not create "+username+" user in "+adminDatabaseName);
				}
			}

			try (MongoClient client = client(runningMongodProcess.getServerAddress(), MongoCredential.createCredential(
				username, adminDatabaseName, password
			))) {
				if (!createUser(client.getDatabase(databaseName), username, password, "readWrite")) {
					throw new IllegalArgumentException("could not create "+username+" in "+databaseName);
				}
			}

			try (MongoClient client = client(runningMongodProcess.getServerAddress(), MongoCredential.createCredential(
				username, "test", password
			))) {
				// if this does not fail, setup is done
				client.getDatabase(databaseName).listCollectionNames().into(new ArrayList<>());
			}
			logger.info("access for "+username+"@"+databaseName+" is enabled");
		};
	}

	@Override
	protected Consumer<RunningMongodProcess> sendShutdown(String username, char[] password, String databaseName) {
		return runningMongodProcess -> {
			logger.info("enable "+username+" access for "+databaseName+" - shutdown database");

			String adminDatabaseName = "admin";

			try (MongoClient client = client(runningMongodProcess.getServerAddress(), MongoCredential.createCredential(
				username, adminDatabaseName, password
			))) {
				client.getDatabase(adminDatabaseName).runCommand(new Document()
					.append("shutdown", 1).append("force", true));
			}
			logger.info("access for "+username+"@"+databaseName+" is enabled - shutdown done");
			runningMongodProcess.shutDownCommandAlreadyExecuted();
		};
	}

	private static boolean createUser(MongoDatabase db, String username, char[] password, String ... roles) {
		Document result = db.runCommand(new Document()
			.append("createUser", username)
			.append("pwd", new String(password))
			.append("roles", Arrays.asList(roles))
		);
		return result.get("ok", Double.class) >= 1.0;
	}
}
