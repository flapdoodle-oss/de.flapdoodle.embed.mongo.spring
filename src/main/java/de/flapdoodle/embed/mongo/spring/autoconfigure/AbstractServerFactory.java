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

import com.mongodb.MongoCredential;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongoProcess;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.types.Try;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class AbstractServerFactory<C extends Closeable> {
	private static Logger logger = LoggerFactory.getLogger(AbstractServerFactory.class);

	protected final MongoProperties properties;
	protected AbstractServerFactory(MongoProperties properties) {
		this.properties = properties;
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
		Listener.TypedListener.Builder typedBuilder = Listener.typedBuilder();
		String username = properties.getUsername();
		char[] password = properties.getPassword();
		String databaseName = properties.getMongoClientDatabase();

		if (username !=null && password !=null) {
			typedBuilder.onStateReached(StateID.of(RunningMongodProcess.class),
				executeClientActions(createAdminUserWithDatabaseAccess(username, password, databaseName)));
			typedBuilder.onStateTearDown(StateID.of(RunningMongodProcess.class),
				executeClientActions(Collections.singletonList(shutdown(username, password)))
					.andThen(RunningMongoProcess::shutDownCommandAlreadyExecuted));
		}
		return typedBuilder.build();
	}

	private Listener initReplicaSet(IFeatureAwareVersion version, MongoProperties properties, MongodArguments mongodArguments) {
		Listener.TypedListener.Builder builder = Listener.typedBuilder();
		String username = properties.getUsername();
		char[] password = properties.getPassword();

		Optional<Storage> replication = mongodArguments.replication();

		Optional<MongoClientAction.Credentials> credentials = username != null
			? Optional.of(MongoClientAction.credentials("admin", username, password))
			: Optional.empty();

		if (replication.isPresent() && version.enabled(Feature.RS_INITIATE)) {
			Consumer<RunningMongodProcess> initReplicaSet = runningMongodProcess -> {
				ServerAddress serverAddress = runningMongodProcess.getServerAddress();
				executeClientAction(runningMongodProcess,
					MongoClientAction.runCommand("admin",
							new Document("replSetInitiate",
								new Document("_id", replication.get().getReplSetName())
									.append("members", Collections.singletonList(
										new Document("_id", 0)
											.append("host", serverAddress.getHost() + ":" + serverAddress.getPort())
									))))
						.withCredentials(credentials)
				);
			};

			builder.onStateReached(StateID.of(RunningMongodProcess.class), initReplicaSet.andThen(runningMongodProcess -> {
				AtomicBoolean isMaster=new AtomicBoolean();
				MongoClientAction checkIfMaster = MongoClientAction.runCommand("admin", new Document("isMaster", 1))
					.withOnResult(doc -> isMaster.set(doc.getBoolean("ismaster")))
					.withCredentials(credentials);

				long started=System.currentTimeMillis();
				long diff;
				do {
					executeClientAction(runningMongodProcess, checkIfMaster);
					diff=System.currentTimeMillis()-started;
					logger.info("check if server is elected as master: {} (after {} ms)", isMaster.get(), diff);
					Try.run(() ->Thread.sleep(100));
				} while (!isMaster.get() && diff<1000);

				if (!isMaster.get()) {
					throw new IllegalArgumentException("initReplicaSet failed to elect "+runningMongodProcess.getServerAddress()+" as master after "+Duration.ofMillis(diff));
				}

			}));
		}

		return builder.build();
	}


	private Consumer<RunningMongodProcess> executeClientActions(List<? extends MongoClientAction> actions) {
		return runningMongodProcess -> executeClientActions(runningMongodProcess, actions);
	}
	
	private void executeClientActions(RunningMongodProcess runningMongodProcess, List<? extends MongoClientAction> actions) {
		for (MongoClientAction action : actions) {
			executeClientAction(runningMongodProcess, action);
		}
	}

	private void executeClientAction(RunningMongodProcess runningMongodProcess, MongoClientAction action) {
		try (C client = action.credentials()
			.map(c -> client(runningMongodProcess.getServerAddress(),
				MongoCredential.createCredential(c.username(), c.database(), c.password().toCharArray())))
			.orElseGet(() -> client(runningMongodProcess.getServerAddress()))) {

			logger.info("credentials: {}, action: {}", action.credentials(), action.action());

			action.onResult()
				.accept(resultOfAction(client, action.action()));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (RuntimeException rx) {
			action.onError().accept(rx);
		}
	}

	protected abstract C client(ServerAddress serverAddress);

	protected abstract C client(ServerAddress serverAddress, MongoCredential credential);

	protected abstract Document resultOfAction(C client, MongoClientAction.Action action);

	private static List<? extends MongoClientAction> createAdminUserWithDatabaseAccess(String username, char[] password, String databaseName) {
		List<ImmutableMongoClientAction> actions = Arrays.asList(
			MongoClientAction.createUser("admin", username, password, "root"),
			MongoClientAction.createUser(databaseName, username, password, "readWrite")
				.withCredentials(MongoClientAction.credentials("admin", username, password)),
			// test list collections
			MongoClientAction.runCommand(databaseName, MongoClientAction.listCollections())
				.withCredentials(MongoClientAction.credentials(databaseName, username, password))
		);
		return actions;
	}

	private static MongoClientAction shutdown(String username, char[] password) {
		return MongoClientAction.shutdown("admin")
			.withCredentials(MongoClientAction.credentials("admin", username, password))
			.withOnError(ex -> logger.debug("expected send shutdown exception", ex));
	}
}
