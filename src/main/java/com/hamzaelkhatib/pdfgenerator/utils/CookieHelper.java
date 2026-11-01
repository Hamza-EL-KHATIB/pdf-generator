package com.hamzaelkhatib.pdfgenerator.utils;

import com.hamzaelkhatib.pdfgenerator.config.ConfigProperties;
import com.microsoft.playwright.options.Cookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CookieHelper {

	private final ConfigProperties configProperties;

	public CookieHelper(ConfigProperties configProperties) {
		this.configProperties = configProperties;
	}

	public List<Cookie> parseCookieString(String cookieString) {
		return Arrays.stream(cookieString.split(";")).map(String::trim).map(cookie -> {
			final String[] parts = cookie.split("=", 2);
			final String name = parts[0];
			final String value = parts.length > 1 ? parts[1] : "";
			return new Cookie(name, value).setDomain(this.configProperties.getRanking().getDomain()).setPath("/")
					.setSecure(true).setHttpOnly(true);
		}).toList();
	}
}
