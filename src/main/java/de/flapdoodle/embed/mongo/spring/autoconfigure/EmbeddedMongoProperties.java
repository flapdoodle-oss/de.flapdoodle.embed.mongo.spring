package de.flapdoodle.embed.mongo.spring.autoconfigure;

import java.util.Set;

import de.flapdoodle.embed.mongo.distribution.Feature;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * Configuration properties for Embedded Mongo.
 *
 * @author Andy Wilkinson
 * @author Yogesh Lonkar
 * @author Chris Bono
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.mongodb.embedded")
public class EmbeddedMongoProperties {

	/**
	 * Version of Mongo to use.
	 */
	private String version;

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

	public EmbeddedMongoProperties.Storage getStorage() {
		return this.storage;
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

		/**
		 * Directory used for data storage.
		 */
		private String databaseDir;

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

		public String getDatabaseDir() {
			return this.databaseDir;
		}

		public void setDatabaseDir(String databaseDir) {
			this.databaseDir = databaseDir;
		}

	}

}
