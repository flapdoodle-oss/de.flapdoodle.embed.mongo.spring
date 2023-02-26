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

import org.bson.Document;
import org.immutables.value.Value;

import java.util.*;
import java.util.function.Consumer;

@Value.Immutable
abstract class MongoClientAction {
	public abstract Optional<Credentials> credentials();
	public abstract Action action();

	@Value.Default
	public Consumer<Document> onResult() {
		return result -> {
			if (result.get("ok", Double.class) < 1.0) {
				throw new IllegalArgumentException(""+action()+" failed with "+result);
			};
		};
	}

	@Value.Default
	public Consumer<RuntimeException> onError() {
		return ex -> {
			throw new RuntimeException(""+action()+" failed", ex);
		};
	}

	@Value.Immutable
	static abstract class Credentials {
		public abstract String database();
		public abstract String username();
		public abstract String password();
	}

	static abstract class Action {
		@Value.Parameter
		public abstract String database();
	}

	@Value.Immutable
	static abstract class RunCommand extends Action {
		@Value.Parameter
		public abstract Document command();
	}

	static ImmutableMongoClientAction createUser(String database, String username, char[] password, String ... roles) {
		return runCommand(database, createUser(username, new String(password), roles));
	}

	static ImmutableMongoClientAction shutdown(String database) {
		return runCommand(database, shutdownForced());
	}

	static ImmutableMongoClientAction runCommand(String database, Document command) {
		return of(ImmutableRunCommand.of(database, command));
	}

	static Credentials credentials(String database, String username, char[] password) {
		return ImmutableCredentials.builder()
			.database(database)
			.username(username)
			.password(new String(password))
			.build();
	}

	static ImmutableMongoClientAction.Builder builder() {
		return ImmutableMongoClientAction.builder();
	}

	static ImmutableMongoClientAction of(MongoClientAction.Action action) {
		return ImmutableMongoClientAction.builder()
			.action(action)
			.build();
	}

	private static Document createUser(String username, String pwd, String ... roles) {
		return new Document("createUser", username)
			.append("pwd", pwd)
			.append("roles", Arrays.asList(roles));
	}

	private static Document role(String rolename, String database) {
		return new Document("role", rolename).append("db", database);
	}

	private static Document createUser(String username, String pwd, Document ... roles) {
		return new Document("createUser", username)
			.append("pwd", pwd)
			.append("roles", Arrays.asList(roles));
	}

	private static Document privilege(String database, String ... actions) {
		return new Document("resource",
			new Document("db", database)
				.append("collection", ""))
			.append("actions", Arrays.asList(actions));
	}

	private static Document createRole(String roleName, Collection<String> roles, Document ... privileges) {
		return new Document("createRole", roleName)
			.append("privileges", Arrays.asList(privileges))
			.append("roles", new ArrayList<>(roles));
	}

	private static Document createRole(String roleName, Document ... privileges) {
		return createRole(roleName, Collections.emptyList(), privileges);
	}

	private static Document shutdownForced() {
		return new Document().append("shutdown", 1).append("force", true);
	}

	static Document listCollections() {
		return new Document("listCollections", 1);
	}
}
