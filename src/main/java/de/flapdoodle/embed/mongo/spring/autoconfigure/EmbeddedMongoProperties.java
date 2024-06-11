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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(prefix = "de.flapdoodle.mongodb.embedded")
public class EmbeddedMongoProperties {

	/**
	 * Version of Mongo to use.
	 */
	private String version;

	private String databaseDir;

	private long startTimeout;

	private final EmbeddedMongoProperties.Storage storage = new EmbeddedMongoProperties.Storage();

	/**
	 * Comma-separated list of features to enable. Uses the defaults of the configured
	 * version by default.
	 */
	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDatabaseDir() {
		return databaseDir;
	}

	public void setDatabaseDir(String databaseDir) {
		this.databaseDir = databaseDir;
	}

	public long getStarttimeout() {
		return startTimeout;
	}

	public void setStarttimeout(long startTimeout) {
		this.startTimeout = startTimeout;
	}

	public EmbeddedMongoProperties.Storage getStorage() {
		return this.storage;
	}

	@Override public String toString() {
		return "EmbeddedMongoProperties{" +
			"version='" + version + '\'' +
			", databaseDir='" + databaseDir + '\'' +
			", storage=" + storage +
			'}';
	}
	public static class Storage {

		/**
		 * Maximum size of the oplog.
		 */
		@DataSizeUnit(DataUnit.MEGABYTES)
		private DataSize oplogSize;

		/**
		 * Name of the replica set.
		 */
		private String replSetName;

		public DataSize getOplogSize() {
			return this.oplogSize;
		}

		public void setOplogSize(DataSize oplogSize) {
			this.oplogSize = oplogSize;
		}

		public String getReplSetName() {
			return this.replSetName;
		}

		public void setReplSetName(String replSetName) {
			this.replSetName = replSetName;
		}

		@Override public String toString() {
			return "Storage{" +
				"oplogSize=" + oplogSize +
				", replSetName='" + replSetName + '\'' +
				'}';
		}
	}

}
