package com.samsepiol.portfolio.provider.github;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.constants.HttpConstants;
import com.samsepiol.library.http.request.ApiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public final class GitHubActivityClient {
    private static final String SERVICE = "github";
    private static final String PUBLIC_EVENTS = "public-events";
    private final HttpClient httpClient;

    public GitHubActivityFetchResponse fetchPublicEvents(char[] token, String etag) {
        var headers = new HashMap<String, String>();
        headers.put(HttpConstants.Headers.AUTHORIZATION, "Bearer " + new String(token));
        headers.put("Accept", "application/vnd.github+json");
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        headers.put("User-Agent", "showoff-github-refresh");
        if (etag != null && !etag.isBlank()) {
            headers.put("If-None-Match", etag);
        }
        var response = httpClient.executeWithResponse(ApiRequest.builder()
                .service(SERVICE)
                .api(PUBLIC_EVENTS)
                .headers(headers)
                .build(), GitHubPublicEventResponse[].class);
        return GitHubActivityFetchResponse.builder()
                .statusCode(response.getStatusCode())
                .etag(response.firstHeader("etag").orElse(null))
                .events(response.getBody() == null ? List.of() : Arrays.asList(response.getBody()))
                .build();
    }
}
