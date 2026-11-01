package com.hamzaelkhatib.pdfgenerator.service;

import com.hamzaelkhatib.pdfgenerator.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

@Service
@Slf4j
public class FileCleanupService {

	private final ConfigProperties configProperties;

	@Autowired
	public FileCleanupService(ConfigProperties configProperties) {
		this.configProperties = configProperties;
	}

	@Scheduled(fixedDelayString = "${pdf.cleanup.cleanup-interval-ms:600000}")
	public void cleanupOldFiles() {
		try {
			final long retentionMinutes = this.configProperties.getCleanup().getFileRetentionDurationMs() / 60000;
			log.info("Running cleanup for files older than {} minutes.", retentionMinutes);

			Files.list(Paths.get(this.configProperties.getStorage().getPath())).filter(Files::isRegularFile)
					.filter(path -> this.isOlderThanRetention(path,
							this.configProperties.getCleanup().getFileRetentionDurationMs()))
					.forEach(this::deleteFileSafely);
		} catch (final IOException e) {
			log.error("Error during cleanup", e);
		}
	}

	private boolean isOlderThanRetention(Path path, long retentionMillis) {
		try {
			final FileTime fileTime = Files.getLastModifiedTime(path);
			return fileTime.toInstant().isBefore(Instant.now().minusMillis(retentionMillis));
		} catch (final IOException e) {
			log.warn("Failed to get last modified time for {}", path, e);
			return false; // Skip file if there's an error
		}
	}

	private void deleteFileSafely(Path path) {
		try {
			Files.deleteIfExists(path);
			log.info("Deleted old file: {}", path);
		} catch (final IOException e) {
			log.error("Failed to delete file: {}", path, e);
		}
	}
}
