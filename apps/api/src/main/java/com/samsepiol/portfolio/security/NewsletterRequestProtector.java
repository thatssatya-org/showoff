package com.samsepiol.portfolio.security;

import com.samsepiol.library.cache.Cache;
import com.samsepiol.library.guava.GuavaCache;
import com.samsepiol.portfolio.api.NewsletterSubscriptionRequest;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

@Component
public final class NewsletterRequestProtector {
    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Cache<String, RequestWindow> clientWindows = GuavaCache.getInstance(WINDOW);
    private final Cache<String, Boolean> acceptedRequests = GuavaCache.getInstance(Duration.ofDays(1));

    public synchronized boolean allow(@NonNull String clientAddress) {
        var clientKey = hash(clientAddress);
        var current = clientWindows.get(clientKey);
        if (current != null && current.count() >= MAX_REQUESTS_PER_WINDOW) {
            return false;
        }
        clientWindows.put(clientKey, new RequestWindow(current == null ? 1 : current.count() + 1));
        return true;
    }

    public boolean wasAccepted(@NonNull NewsletterSubscriptionRequest request) {
        return acceptedRequests.get(correlationKey(request)) != null;
    }

    public void recordAccepted(@NonNull NewsletterSubscriptionRequest request) {
        acceptedRequests.put(correlationKey(request), Boolean.TRUE);
    }

    private static @NonNull String correlationKey(NewsletterSubscriptionRequest request) {
        return hash(request.email() + "\u0000" + request.consentVersion());
    }

    private static @NonNull String hash(@NonNull String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private record RequestWindow(int count) {
    }
}
