package com.samsepiol.portfolio.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record NewsletterSubscriptionRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 128) String consentVersion,
        @Size(max = 128) String source,
        @Size(max = 256) String website) {

    public NewsletterSubscriptionRequest {
        email = normalizeEmail(email);
        consentVersion = trimToNull(consentVersion);
        source = trimToNull(source);
        website = trimToNull(website);
    }

    private static String normalizeEmail(String value) {
        var trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
