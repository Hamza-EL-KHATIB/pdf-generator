package com.hamzaelkhatib.pdfgenerator.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

	@Value("${pdf.executor.pool-size}")
	private int poolSize;

	@Value("${pdf.ranking.batch-size}")
	private int batchSize;

	/** Renders the chunks of one batch at the same time. */
	@Bean(name = "pdfChunkExecutor")
	public Executor pdfChunkExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(this.batchSize);
		executor.setMaxPoolSize(this.batchSize);
		executor.setThreadNamePrefix("pdf-chunk-");
		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(this.poolSize);
		executor.setMaxPoolSize(this.poolSize);
		executor.setQueueCapacity(500); // Size of waiting queue
		executor.setThreadNamePrefix("pdf-generator-");
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new SimpleAsyncUncaughtExceptionHandler();
	}
}