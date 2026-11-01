package com.hamzaelkhatib.pdfgenerator.controller;

import com.hamzaelkhatib.pdfgenerator.config.ConfigProperties;
import com.hamzaelkhatib.pdfgenerator.service.PdfGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class PdfController {

	private final PdfGeneratorService pdfGeneratorService;

	private final ConfigProperties configProperties;

	public PdfController(PdfGeneratorService pdfGeneratorService, ConfigProperties configProperties) {
		this.pdfGeneratorService = pdfGeneratorService;
		this.configProperties = configProperties;
	}

	@Value("${cors.allowed-origin}")
	private String allowedOrigin;

	@GetMapping("/generateRankingPdf")
	public ResponseEntity<Map<String, String>> generatePdf(@RequestParam String dataUrl,
			@RequestParam int numberOfArticles, HttpServletRequest request) {

		final String taskId = UUID.randomUUID().toString();
		final String cookie = request.getHeader("Cookie");

		try {
			this.pdfGeneratorService.createPendingFile(taskId);

			// Pass cookie to service
			this.pdfGeneratorService.generatePdfAsync(taskId, dataUrl, numberOfArticles, cookie);

			final HttpHeaders headers = new HttpHeaders();
			headers.set("Access-Control-Allow-Credentials", "true");
			headers.set("Access-Control-Expose-Headers", "X-Auth-Token");
			headers.set("Access-Control-Allow-Origin", this.allowedOrigin);
			headers.set("Vary", "Origin");

			final Map<String, String> response = new HashMap<>();
			response.put("taskId", "/api/v1/checkPdfStatus?taskId=" + taskId);

			return ResponseEntity.ok().headers(headers).body(response);

		} catch (final IOException e) {
			log.error("Error creating pending file", e);
			throw new RuntimeException("Failed to initialize PDF generation", e);
		}
	}

	@GetMapping("/checkPdfStatus")
	public ResponseEntity<?> checkStatus(@RequestParam String taskId) {
		final Path pendingPath = Paths.get(this.configProperties.getStorage().getPath(), taskId + "-pending.pdf");
		final Path completedPath = Paths.get(this.configProperties.getStorage().getPath(), taskId + "-completed.pdf");

		if (Files.exists(completedPath)) {

			// Create resource from completed file
			final Resource resource = new FileSystemResource(completedPath.toFile());

			// Set up response
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + taskId + ".pdf\"")
					.header("Access-Control-Allow-Origin", this.allowedOrigin)
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Expose-Headers", "X-Auth-Token").header("Vary", "Origin").body(resource);

		} else if (Files.exists(pendingPath)) {
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.ok("File removed or does not exist");
		}
	}
}
