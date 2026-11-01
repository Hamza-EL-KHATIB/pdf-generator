package com.hamzaelkhatib.pdfgenerator.service;

import com.hamzaelkhatib.pdfgenerator.config.ConfigProperties;
import com.hamzaelkhatib.pdfgenerator.utils.PdfUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGeneratorService {

	@Autowired
	PdfTaskManager pdfTaskManager;

	@Autowired
	PdfUtils pdfUtils;

	@Autowired
	@Qualifier("pdfChunkExecutor")
	Executor chunkExecutor;

	private final ConfigProperties configProperties;

	@PostConstruct
	public void init() {
		try {
			Files.createDirectories(Paths.get(this.configProperties.getStorage().getPath()));
		} catch (final IOException e) {
			log.error("Failed to create storage directory", e);
		}
	}

	public void createPendingFile(String taskId) throws IOException {
		final Path pendingPath = Paths.get(this.configProperties.getStorage().getPath(), taskId + "-pending.pdf");
		Files.createFile(pendingPath);
	}

	@Async
	public void generatePdfAsync(String taskId, String dataUrl, int numberOfArticles, String cookie) {
		try {
			// Generate all URLs for chunks
			final List<String> urls = this.pdfTaskManager.generateUrls(dataUrl, numberOfArticles);
			log.info("Generated {} chunk URLs for {} articles", urls.size(), numberOfArticles);

			// List to store successfully generated chunks
			final List<Path> successfulChunks = new ArrayList<>();

			// Process chunks in sequential batches
			for (int startIndex = 0; startIndex < urls.size(); startIndex += this.configProperties.getRanking()
					.getBatchSize()) {
				final int currentBatchStart = startIndex;
				final List<String> batchUrls = urls.subList(startIndex,
						Math.min(startIndex + this.configProperties.getRanking().getBatchSize(), urls.size()));

				// Process each batch asynchronously
				final List<CompletableFuture<Path>> batchFutures = IntStream.range(0, batchUrls.size())
						.mapToObj(i -> CompletableFuture.supplyAsync(() -> this.pdfTaskManager.generateChunk(taskId,
								batchUrls.get(i), currentBatchStart + i, cookie), this.chunkExecutor))
						.toList();

				// Wait for this batch to complete
				CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

				// Collect paths of successfully generated chunks
				for (final CompletableFuture<Path> future : batchFutures) {
					try {
						final Path chunkPath = future.get();
						if (chunkPath != null && Files.exists(chunkPath)) {
							successfulChunks.add(chunkPath);
							log.info("Successfully generated chunk: {}", chunkPath.getFileName());
						}
					} catch (final Exception e) {
						log.error("Failed to get chunk result", e);
					}
				}
			}

			// Only merge if we have successful chunks
			if (!successfulChunks.isEmpty()) {
				log.info("Starting merge of {} chunks", successfulChunks.size());
				final Path completedPath = Paths.get(this.configProperties.getStorage().getPath(),
						taskId + "-completed.pdf");

				// Execute synchronous merging
				final boolean mergeSuccess = this.pdfUtils.mergeChunks(successfulChunks, completedPath);

				if (mergeSuccess) {
					log.info("Successfully merged {} chunks into final PDF", successfulChunks.size());

					// Cleanup chunks after merging
					for (final Path chunk : successfulChunks) {
						try {
							Files.deleteIfExists(chunk);
						} catch (final IOException e) {
							log.warn("Failed to delete chunk: {}", chunk, e);
						}
					}

					// Remove the pending file
					Files.deleteIfExists(
							Paths.get(this.configProperties.getStorage().getPath(), taskId + "-pending.pdf"));
				}
			} else {
				log.error("No chunks were successfully generated for taskId: {}", taskId);
			}

		} catch (final Exception e) {
			log.error("Error in PDF generation process for taskId: {}", taskId, e);
		}
	}

}