package com.samsepiol.portfolio.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.response.HttpResponseEnvelope;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenUse;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.repository.GitHubActivitySnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitHubActivitySnapshotStrategyTest {
    @Test
    void sendsTheStoredEtagAndAtomicallyReplacesOnlyThePublicEventProjection() throws Exception {
        var httpClient = mock(HttpClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubActivitySnapshotRepository.class);
        var properties = new GitHubRefreshProperties(true, true, "github-primary", "octocat", "thatssatya-org", "easyfintrack", "0 */15 * * * *");
        var tokenProperties = new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_ACTIVITY).profileId("github-primary")
                .state(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY).title("Recent public activity")
                .sourceLabel("GitHub").refreshedAt(java.time.Instant.EPOCH).validUntil(java.time.Instant.EPOCH)
                .content(Map.of("events", List.of())).publicApproved(true).profileEnabled(true).providerEtag("\"prior\"").build();
        when(repository.find("github-primary")).thenReturn(Optional.of(existing));
        when(httpClient.executeWithResponse(any(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(HttpResponseEnvelope.<String>builder().statusCode(200).headers(Map.of("ETag", List.of("\"next\"")))
                                .body("[{\"type\":\"PushEvent\",\"created_at\":\"2026-08-23T12:34:56Z\",\"repo\":{\"name\":\"owner/public-repo\"},\"payload\":{\"commits\":[{\"message\":\"never persist\"}]}}]").build(),
                        HttpResponseEnvelope.<String>builder().statusCode(200).headers(Map.of()).body("{\"name\":\"easyfintrack\",\"html_url\":\"https://github.com/thatssatya-org/easyfintrack\",\"stargazers_count\":7,\"language\":\"JavaScript\",\"topics\":[\"finance\"],\"updated_at\":\"2026-08-23T12:34:56Z\",\"description\":\"ledger\"}").build());
        when(httpClient.executeWithResponse(any(), org.mockito.ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(HttpResponseEnvelope.<com.fasterxml.jackson.databind.JsonNode>builder().statusCode(200).headers(Map.of()).body(new ObjectMapper().readTree("{\"data\":{\"viewer\":{\"contributionsCollection\":{\"contributionCalendar\":{\"totalContributions\":42,\"weeks\":[{\"contributionDays\":[{\"date\":\"2026-08-23\",\"contributionCount\":2}]}]}}}}}")).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var strategy = new GitHubActivitySnapshotStrategy(httpClient, tokenManagementService, repository, properties,
                tokenProperties, new ObjectMapper());
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());

        var request = ArgumentCaptor.forClass(com.samsepiol.library.http.request.ApiRequest.class);
        verify(httpClient, org.mockito.Mockito.times(2)).executeWithResponse(request.capture(), org.mockito.ArgumentMatchers.eq(String.class));
        assertThat(request.getAllValues().getFirst().getHeaders()).containsEntry("If-None-Match", "\"prior\"")
                .containsEntry("Authorization", "Bearer token-not-to-log");
        var replacement = ArgumentCaptor.forClass(ExternalSnapshotEntity.class);
        verify(repository).replace(replacement.capture());
        assertThat(replacement.getValue().getProviderEtag()).isEqualTo("\"next\"");
        assertThat(replacement.getValue().getContent()).containsKeys("events", "contributions", "repositories")
                .doesNotContainValue("never persist").doesNotContainValue("payload");
        assertThat(replacement.getValue().getContent().get("events").toString()).contains("PushEvent", "2026-08-23", "owner/public-repo")
                .doesNotContain("never persist", "payload");
        assertThat(result.getSnapshot().getContent()).isEqualTo(replacement.getValue().getContent());
    }

    @Test
    void leavesTheLastKnownGoodSnapshotUntouchedOnNotModified() throws Exception {
        var httpClient = mock(HttpClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubActivitySnapshotRepository.class);
        var properties = new GitHubRefreshProperties(true, true, "github-primary", "octocat", "thatssatya-org", "easyfintrack", "0 */15 * * * *");
        var tokenProperties = new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_ACTIVITY).profileId("github-primary")
                .state(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY).title("Recent public activity")
                .sourceLabel("GitHub").refreshedAt(java.time.Instant.EPOCH).validUntil(java.time.Instant.EPOCH)
                .content(Map.of("events", List.of())).publicApproved(true).profileEnabled(true).providerEtag("\"prior\"").build();
        when(repository.find("github-primary")).thenReturn(Optional.of(existing));
        when(httpClient.executeWithResponse(any(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(HttpResponseEnvelope.<String>builder().statusCode(304).headers(Map.of("ETag", List.of("\"prior\""))).body("").build(),
                        HttpResponseEnvelope.<String>builder().statusCode(200).headers(Map.of()).body("{\"name\":\"easyfintrack\",\"html_url\":\"https://github.com/thatssatya-org/easyfintrack\",\"stargazers_count\":7,\"topics\":[]}").build());
        when(httpClient.executeWithResponse(any(), org.mockito.ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(HttpResponseEnvelope.<com.fasterxml.jackson.databind.JsonNode>builder().statusCode(200).headers(Map.of()).body(new ObjectMapper().readTree("{\"data\":{\"viewer\":{\"contributionsCollection\":{\"contributionCalendar\":{\"totalContributions\":42,\"weeks\":[]}}}}}")).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var strategy = new GitHubActivitySnapshotStrategy(httpClient, tokenManagementService, repository, properties,
                tokenProperties, new ObjectMapper());
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());

        org.mockito.Mockito.verify(repository).replace(any());
        assertThat(result.getSnapshot().getContent().get("events")).isEqualTo(List.of());
    }
}
