package com.samsepiol.portfolio.application;

import com.samsepiol.portfolio.api.NewsletterServiceUnavailableException;
import com.samsepiol.portfolio.api.NewsletterSubscriptionRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnavailableNewsletterSubscriptionServiceTest {
    @Test
    void failsClosedWhenListmonkIsNotConfigured() {
        var service = new UnavailableNewsletterSubscriptionService();

        assertThatThrownBy(() -> service.subscribe(new NewsletterSubscriptionRequest(
                "visitor@example.com", "2026-08", null, null)))
                .isInstanceOf(NewsletterServiceUnavailableException.class);
    }
}
