package com.samsepiol.portfolio.provider.github;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.response.HttpResponseEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultGithubServiceClientTest {
    @Test
    void fetchesTypedEventsWithTheStoredEtag() {
        var httpClient = mock(HttpClient.class);
        var event = GitHubPublicEventResponse.builder().type("PushEvent").createdAt("2026-08-23T12:34:56Z")
                .repo(GitHubRepositoryResponse.builder().name("owner/public-repo").build())
                .build();
        when(httpClient.executeWithResponse(any(), eq(GitHubPublicEventResponse[].class)))
                .thenReturn(HttpResponseEnvelope.<GitHubPublicEventResponse[]>builder().statusCode(200)
                        .headers(Map.of("ETag", List.of("\"next\"")))
                        .body(new GitHubPublicEventResponse[]{event})
                        .build());

        var response = new DefaultGithubServiceClient(httpClient).fetchPublicEvents("token-not-to-log".toCharArray(), "\"prior\"");

        var request = ArgumentCaptor.forClass(com.samsepiol.library.http.request.ApiRequest.class);
        verify(httpClient).executeWithResponse(request.capture(), eq(GitHubPublicEventResponse[].class));
        assertThat(request.getValue().getHeaders()).containsEntry("If-None-Match", "\"prior\"")
                .containsEntry("Authorization", "Bearer token-not-to-log");
        assertThat(response.getEtag()).isEqualTo("\"next\"");
        assertThat(response.getEvents()).containsExactly(event);
    }

    @Test
    void fetchesOnlyTheContributionCalendarAggregateThroughGraphQl() {
        var httpClient = mock(HttpClient.class);
        var contributionDay = GitHubContributionDayResponse.builder().date("2026-08-23").contributionCount(4).build();
        var calendar = GitHubContributionCalendarResponse.builder().totalContributions(4)
                .weeks(List.of(GitHubContributionWeekResponse.builder().contributionDays(List.of(contributionDay)).build()))
                .build();
        var responseBody = GitHubContributionGraphQlResponse.builder()
                .data(GitHubContributionGraphQlDataResponse.builder()
                        .viewer(GitHubContributionViewerResponse.builder()
                                .login("octocat")
                                .contributionsCollection(GitHubContributionsCollectionResponse.builder()
                                        .contributionCalendar(calendar).build())
                                .build())
                        .build())
                .build();
        when(httpClient.executeWithResponse(any(), eq(GitHubContributionGraphQlResponse.class)))
                .thenReturn(HttpResponseEnvelope.<GitHubContributionGraphQlResponse>builder().statusCode(200)
                        .headers(Map.of()).body(responseBody).build());

        var response = new DefaultGithubServiceClient(httpClient).fetchContributionCalendar("token-not-to-log".toCharArray(),
                "octocat", Instant.parse("2025-08-24T00:00:00Z"), Instant.parse("2026-08-24T00:00:00Z"));

        var request = ArgumentCaptor.forClass(com.samsepiol.library.http.request.ApiRequest.class);
        verify(httpClient).executeWithResponse(request.capture(), eq(GitHubContributionGraphQlResponse.class));
        assertThat(request.getValue().getApi()).isEqualTo(GithubServiceClient.Constants.CONTRIBUTION_CALENDAR);
        assertThat(request.getValue().getHeaders()).containsEntry("Authorization", "Bearer token-not-to-log")
                .containsEntry("Content-Type", "application/json");
        assertThat(request.getValue().getBody()).isInstanceOf(GitHubContributionCalendarRequest.class);
        var body = (GitHubContributionCalendarRequest) request.getValue().getBody();
        assertThat(body.getQuery()).contains("viewer", "login", "contributionCalendar", "totalContributions", "contributionDays")
                .doesNotContain("repositories", "commitContributionsByRepository", "issueContributions");
        assertThat(body.getVariables()).containsExactlyEntriesOf(Map.of("from", "2025-08-24T00:00:00Z",
                "to", "2026-08-24T00:00:00Z"));
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.getContributionCalendar()).isEqualTo(calendar);
    }
}
