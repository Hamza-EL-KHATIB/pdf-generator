package com.hamzaelkhatib.pdfgenerator.service;

import com.hamzaelkhatib.pdfgenerator.config.ConfigProperties;
import com.hamzaelkhatib.pdfgenerator.utils.CookieHelper;
import com.hamzaelkhatib.pdfgenerator.utils.PdfUtils;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Margin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PdfTaskManager {

	private final ConfigProperties configProperties;

	@Autowired
	CookieHelper cookieHelper;

	@Autowired
	PdfUtils pdfUtils;

	public PdfTaskManager(ConfigProperties configProperties) {
		this.configProperties = configProperties;
	}

	public List<String> generateUrls(String dataUrl, int numberOfArticles) {
		final List<String> urls = new ArrayList<>();
		for (int offset = 0; offset < numberOfArticles; offset += this.configProperties.getRanking()
				.getArticlesPerPdf()) {
			final int limit = Math.min(this.configProperties.getRanking().getArticlesPerPdf(),
					numberOfArticles - offset);
			final String url = String.format("%s/%d/%d?dataUrl=%s",
					this.configProperties.getRanking().getRoot(), offset, limit,
					URLEncoder.encode(dataUrl, StandardCharsets.UTF_8));
			urls.add(url);
		}
		return urls;
	}

	public Path generateChunk(String taskId, String url, int index, String cookie) {
		final Path chunkPath = Paths.get(this.configProperties.getStorage().getPath(),
				taskId + "-chunk-" + index + ".pdf");

		try (final Playwright playwright = Playwright.create()) {
			final Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
			final BrowserContext context = browser.newContext();

			if (cookie != null && !cookie.isEmpty()) {
				final List<Cookie> cookies = this.cookieHelper.parseCookieString(cookie);
				context.addCookies(cookies);
				log.info("Added {} cookies to context", cookies.size());
			}

			final Page page = context.newPage();
			page.navigate(url);
			page.waitForLoadState(LoadState.NETWORKIDLE);

			page.waitForFunction("() => new Promise(resolve => {"
					+ "  const images = Array.from(document.querySelectorAll('img'));"
					+ "  if (images.length === 0) resolve();" + "  let loadedCount = 0;" + "  images.forEach(img => {"
					+ "    if (img.complete) {" + "      loadedCount++;"
					+ "      if (loadedCount === images.length) resolve();" + "    } else {"
					+ "      img.addEventListener('load', () => {" + "        loadedCount++;"
					+ "        if (loadedCount === images.length) resolve();" + "      });"
					+ "      img.addEventListener('error', () => {" + "        loadedCount++;"
					+ "        if (loadedCount === images.length) resolve();" + "      });" + "    }" + "  });" + "})");

			page.pdf(new Page.PdfOptions().setPath(chunkPath).setFormat("A4").setScale(0.95).setPrintBackground(true)
					.setMargin(new Margin().setTop("1cm").setRight("1cm").setBottom("1cm").setLeft("1cm")));

			context.close();
			browser.close();

			// Remove the last two pages
			this.pdfUtils.removeLastPages(chunkPath);

			return chunkPath;

		} catch (final Exception e) {
			log.error("Error generating chunk for index: {}", index, e);
			return null;
		}
	}
}
