package com.samsepiol.portfolio.application;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.constants.HttpConstants;
import com.samsepiol.library.http.request.ApiRequest;
import com.samsepiol.library.core.exception.LibraryException;
import com.samsepiol.portfolio.api.NewsletterServiceUnavailableException;
import com.samsepiol.portfolio.api.NewsletterSubscriptionRequest;
import com.samsepiol.portfolio.configuration.ListmonkProperties;
import com.samsepiol.portfolio.configuration.NewsletterSubscriptionConfiguration;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class ListmonkNewsletterSubscriptionService implements NewsletterSubscriptionService {
    private final HttpClient httpClient;
    private final ListmonkProperties properties;

    public ListmonkNewsletterSubscriptionService(@NonNull HttpClient httpClient, @NonNull ListmonkProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    public void subscribe(@NonNull NewsletterSubscriptionRequest request) {
        try {
            var responseStatus = httpClient.execute(ApiRequest.builder()
                    .service(NewsletterSubscriptionConfiguration.ListmonkConfiguration.SERVICE)
                    .api(NewsletterSubscriptionConfiguration.ListmonkConfiguration.CREATE_SUBSCRIBER)
                    .headers(Map.of(HttpConstants.Headers.AUTHORIZATION, basicAuthorization()))
                    .body(ListmonkSubscriberRequest.builder()
                            .email(request.email())
                            .status("unconfirmed")
                            .lists(List.of(properties.listId()))
                            .preconfirm(false)
                            .build())
                    .build());
            if (!responseStatus.isSuccessful() && responseStatus.getStatusCode() != 409) {
                throw new NewsletterServiceUnavailableException();
            }
        } catch (NewsletterServiceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new NewsletterServiceUnavailableException();
        }
    }

    private @NonNull String basicAuthorization() {
        var credential = properties.username() + ":" + properties.password();
        return "Basic " + Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    }
}
