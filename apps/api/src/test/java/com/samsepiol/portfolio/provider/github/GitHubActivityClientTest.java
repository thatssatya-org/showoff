package com.samsepiol.portfolio.provider.github;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.response.HttpResponseEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitHubActivityClientTest {
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

        var response = new GitHubActivityClient(httpClient).fetchPublicEvents("token-not-to-log".toCharArray(), "\"prior\"");

        var request = ArgumentCaptor.forClass(com.samsepiol.library.http.request.ApiRequest.class);
        verify(httpClient).executeWithResponse(request.capture(), eq(GitHubPublicEventResponse[].class));
        assertThat(request.getValue().getHeaders()).containsEntry("If-None-Match", "\"prior\"")
                .containsEntry("Authorization", "Bearer token-not-to-log");
        assertThat(response.getEtag()).isEqualTo("\"next\"");
        assertThat(response.getEvents()).containsExactly(event);
    }
}
