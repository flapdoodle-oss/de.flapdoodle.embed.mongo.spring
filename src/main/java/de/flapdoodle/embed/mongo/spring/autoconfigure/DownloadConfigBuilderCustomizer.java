package de.flapdoodle.embed.mongo.spring.autoconfigure;

import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig;

@FunctionalInterface
public interface DownloadConfigBuilderCustomizer {

	/**
	 * Customize the {@link ImmutableDownloadConfig.Builder}.
	 * @param downloadConfigBuilder the {@link ImmutableDownloadConfig.Builder} to
	 * customize
	 */
	void customize(ImmutableDownloadConfig.Builder downloadConfigBuilder);

}