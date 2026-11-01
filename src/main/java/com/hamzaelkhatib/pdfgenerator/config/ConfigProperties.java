package com.hamzaelkhatib.pdfgenerator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pdf")
public class ConfigProperties {

	private Storage storage;
	private Cleanup cleanup;
	private Ranking ranking;
	private Executor executor;
	private int pagesToRemove;

	@Getter
	@Setter
	public static class Storage {
		private String path;
	}

	@Getter
	@Setter
	public static class Cleanup {
		private long fileRetentionDurationMs; // Time files are retained before deletion
		private long cleanupIntervalMs; // Frequency of cleanup checks
	}

	@Getter
	@Setter
	public static class Ranking {
		private String root;
		private String domain;
		private String environment;
		private int articlesPerPdf;
		private int batchSize;
	}

	@Getter
	@Setter
	public static class Executor {
		private int poolSize; // Size of the executor pool
	}
}
