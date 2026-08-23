package com.samsepiol.portfolio.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Component
public class NewsletterOriginValidator {
    private final URI publicBaseUrl;

    public NewsletterOriginValidator(@Value("${portfolio.public-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = parse(publicBaseUrl);
    }

    public boolean isAllowed(String origin) {
        if (origin == null || origin.isBlank()) {
            return true;
        }
        var candidate = parse(origin);
        return candidate != null && publicBaseUrl != null && publicBaseUrl.equals(candidate);
    }

    private static URI parse(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            var uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))) {
                return null;
            }
            return new URI(uri.getScheme().toLowerCase(Locale.ROOT), null,
                    uri.getHost().toLowerCase(Locale.ROOT), uri.getPort(), null, null, null);
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return null;
        }
    }
}
