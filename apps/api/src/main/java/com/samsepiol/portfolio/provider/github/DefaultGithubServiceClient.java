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
public final class DefaultGithubServiceClient implements GithubServiceClient {
    private final HttpClient httpClient;

    public GitHubActivityFetchResponse fetchPublicEvents(char[] token, String etag) {
        var headers = authenticatedHeaders(token);
        if (etag != null && !etag.isBlank()) {
            headers.put(GithubServiceClient.Constants.IF_NONE_MATCH, etag);
        }
        var response = httpClient.executeWithResponse(ApiRequest.builder()
                .service(GithubServiceClient.Constants.SERVICE)
                .api(GithubServiceClient.Constants.PUBLIC_EVENTS)
                .headers(headers)
                .build(), GitHubPublicEventResponse[].class);
        return GitHubActivityFetchResponse.builder()
                .statusCode(response.getStatusCode())
                .etag(response.firstHeader(GithubServiceClient.Constants.ETAG).orElse(null))
                .events(response.getBody() == null ? List.of() : Arrays.asList(response.getBody()))
                .build();
    }

    @Override
    public GitHubContributionFetchResponse fetchContributionCalendar(char[] token, GitHubContributionCalendarRequest request) {
        var headers = authenticatedHeaders(token);
        headers.put(GithubServiceClient.Constants.CONTENT_TYPE, GithubServiceClient.Constants.APPLICATION_JSON);
        var response = httpClient.executeWithResponse(ApiRequest.builder()
                .service(GithubServiceClient.Constants.SERVICE)
                .api(GithubServiceClient.Constants.CONTRIBUTION_CALENDAR)
                .headers(headers)
                .body(request)
                .build(), GitHubContributionGraphQlResponse.class);
        var responseBody = response.getBody();
        return GitHubContributionFetchResponse.builder()
                .statusCode(response.getStatusCode())
                .contributionCalendar(responseBody == null || responseBody.getData() == null
                        || responseBody.getData().getViewer() == null
                        || responseBody.getData().getViewer().getContributionsCollection() == null
                        ? null
                        : responseBody.getData().getViewer().getContributionsCollection().getContributionCalendar())
                .hasErrors(responseBody != null && (responseBody.hasErrors() || !responseBody.viewerMatches(request.getExpectedHandle())))
                .build();
    }

    private HashMap<String, String> authenticatedHeaders(char[] token) {
        var headers = new HashMap<String, String>();
        headers.put(HttpConstants.Headers.AUTHORIZATION, "Bearer " + new String(token));
        headers.put("Accept", GithubServiceClient.Constants.ACCEPT);
        headers.put(GithubServiceClient.Constants.API_VERSION_HEADER, GithubServiceClient.Constants.API_VERSION);
        headers.put(GithubServiceClient.Constants.USER_AGENT, GithubServiceClient.Constants.USER_AGENT_VALUE);
        return headers;
    }
}
