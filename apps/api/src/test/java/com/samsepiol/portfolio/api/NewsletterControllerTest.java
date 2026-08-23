package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.application.NewsletterSubscriptionService;
import com.samsepiol.portfolio.security.NewsletterRequestProtector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NewsletterController.class)
@Import({ApiExceptionHandler.class, NewsletterOriginValidator.class, NewsletterRequestProtector.class,
        NewsletterControllerTest.TestConfig.class})
@TestPropertySource(properties = "portfolio.public-base-url=https://portfolio.example")
class NewsletterControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsletterSubscriptionService newsletterSubscriptionService;

    @BeforeEach
    void resetService() {
        reset(newsletterSubscriptionService);
    }

    @Test
    void acceptsAValidSameOriginRequestAndDoesNotReturnTheEmail() throws Exception {
        mockMvc.perform(post("/api/v1/newsletter/subscriptions")
                        .header("Origin", "https://portfolio.example")
                        .contentType("application/json")
                        .content("{\"email\":\"  PERSON@Example.COM \",\"consentVersion\":\"2026-08\",\"source\":\" newsletter-page \"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PERSON@Example.COM"))));

        var request = ArgumentCaptor.forClass(NewsletterSubscriptionRequest.class);
        verify(newsletterSubscriptionService).subscribe(request.capture());
        assertThat(request.getValue().email()).isEqualTo("person@example.com");
        assertThat(request.getValue().source()).isEqualTo("newsletter-page");
    }

    @Test
    void permitsRequestsWithoutOriginForServerSideOrReverseProxyTraffic() throws Exception {
        mockMvc.perform(validRequest("visitor@example.com"))
                .andExpect(status().isAccepted());
    }

    @Test
    void rejectsCrossOriginRequestsWithoutCallingTheService() throws Exception {
        mockMvc.perform(validRequest("visitor@example.com").header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("The subscription request could not be accepted."));
        verify(newsletterSubscriptionService, never()).subscribe(any());
    }

    @Test
    void rejectsMalformedOriginWithoutCallingTheService() throws Exception {
        mockMvc.perform(validRequest("visitor@example.com").header("Origin", "not an origin"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("The subscription request could not be accepted."));
        verify(newsletterSubscriptionService, never()).subscribe(any());
    }

    @Test
    void rejectsHoneypotAndMalformedRequestsWithoutEchoingInput() throws Exception {
        mockMvc.perform(validRequest("visitor@example.com").content("{\"email\":\"visitor@example.com\",\"consentVersion\":\"2026-08\",\"website\":\"https://bot.example\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("visitor@example.com"))));
        mockMvc.perform(post("/api/v1/newsletter/subscriptions")
                        .contentType("application/json")
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The subscription request could not be accepted."));
        verify(newsletterSubscriptionService, never()).subscribe(any());
    }

    @Test
    void returnsServiceUnavailableWhenTheServiceCannotReachConfiguredDelivery() throws Exception {
        doThrow(new NewsletterServiceUnavailableException()).when(newsletterSubscriptionService).subscribe(any());

        mockMvc.perform(validRequest("unavailable@example.com"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Newsletter unavailable"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRequest(String email) {
        return post("/api/v1/newsletter/subscriptions")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"consentVersion\":\"2026-08\"}");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        NewsletterSubscriptionService newsletterSubscriptionService() {
            return mock(NewsletterSubscriptionService.class);
        }
    }
}
