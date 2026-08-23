package com.samsepiol.portfolio.security;

import com.samsepiol.portfolio.api.NewsletterSubscriptionRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsletterRequestProtectorTest {

    @Test
    void limitsRequestsFromOneClientWithinTheShortWindow() {
        var protector = new NewsletterRequestProtector();

        assertThat(protector.allow("127.0.0.1")).isTrue();
        assertThat(protector.allow("127.0.0.1")).isTrue();
        assertThat(protector.allow("127.0.0.1")).isTrue();
        assertThat(protector.allow("127.0.0.1")).isTrue();
        assertThat(protector.allow("127.0.0.1")).isTrue();
        assertThat(protector.allow("127.0.0.1")).isFalse();
    }

    @Test
    void recognisesARepeatWithoutKeepingThePlaintextEmail() {
        var protector = new NewsletterRequestProtector();
        var request = new NewsletterSubscriptionRequest("PERSON@example.com", "newsletter-v1", null, null);

        assertThat(protector.wasAccepted(request)).isFalse();

        protector.recordAccepted(request);

        assertThat(protector.wasAccepted(new NewsletterSubscriptionRequest(
                "person@example.com", "newsletter-v1", "portfolio", null))).isTrue();
    }
}
