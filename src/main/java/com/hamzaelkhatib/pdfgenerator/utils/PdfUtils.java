package com.hamzaelkhatib.pdfgenerator.utils;

import com.hamzaelkhatib.pdfgenerator.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class PdfUtils {

	private final ConfigProperties configProperties;

	public PdfUtils(ConfigProperties configProperties) {
		this.configProperties = configProperties;
	}

	public void removeLastPages(Path chunkPath) {
		try (final PDDocument doc = PDDocument.load(chunkPath.toFile())) {
			final int totalPages = doc.getNumberOfPages();

			// Remove the specified number of pages from the end, if there are enough pages
			for (int i = 0; i < this.configProperties.getPagesToRemove() && totalPages - 1 - i >= 0; i++) {
				doc.removePage(totalPages - 1 - i);
			}

			doc.save(chunkPath.toFile());

		} catch (final IOException e) {
			log.error("Error removing last pages from chunk: {}", chunkPath, e);
		}
	}

	public boolean mergeChunks(List<Path> chunkPaths, Path outputPath) {
		log.info("Starting PDF merge of {} chunks into {}", chunkPaths.size(), outputPath);

		final PDFMergerUtility merger = new PDFMergerUtility();
		merger.setDestinationFileName(outputPath.toString());

		try {
			// Sort paths by their index to ensure correct order
			chunkPaths.sort(Comparator.comparingInt(this::extractChunkIndex));

			for (final Path chunkPath : chunkPaths) {
				merger.addSource(chunkPath.toFile());
			}

			// Perform the merge
			merger.mergeDocuments(null); // Use null or MemoryUsageSetting.setupMainMemoryOnly() for simplicity
			log.info("Successfully merged PDFs into {}", outputPath);
			return true;

		} catch (final IOException e) {
			log.error("Error during PDF merge", e);
			return false;
		}
	}

	private int extractChunkIndex(Path path) {
		final String filename = path.getFileName().toString();
		final String[] parts = filename.split("-");
		try {
			return Integer.parseInt(parts[parts.length - 1].replace(".pdf", ""));
		} catch (final NumberFormatException e) {
			log.warn("Failed to parse chunk index from filename: {}", filename);
			return -1; // Defaults to lowest priority if parsing fails
		}
	}
}
