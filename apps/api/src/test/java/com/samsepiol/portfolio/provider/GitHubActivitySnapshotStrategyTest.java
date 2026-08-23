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
    void sendsTheStoredEtagAndAtomicallyReplacesOnlyThePublicEventProjection() {
        var httpClient = mock(HttpClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubActivitySnapshotRepository.class);
        var properties = new GitHubRefreshProperties(true, true, "github-primary", "octocat", "0 */15 * * * *");
        var tokenProperties = new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_ACTIVITY).profileId("github-primary")
                .state(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY).title("Recent public activity")
                .sourceLabel("GitHub").refreshedAt(java.time.Instant.EPOCH).validUntil(java.time.Instant.EPOCH)
                .content(Map.of("events", "[]")).publicApproved(true).profileEnabled(true).providerEtag("\"prior\"").build();
        when(repository.find("github-primary")).thenReturn(Optional.of(existing));
        when(httpClient.executeWithResponse(any())).thenReturn(new HttpResponseEnvelope(200, Map.of("ETag", List.of("\"next\"")),
                "[{\"type\":\"PushEvent\",\"created_at\":\"2026-08-23T12:34:56Z\",\"repo\":{\"name\":\"owner/public-repo\"},\"payload\":{\"commits\":[{\"message\":\"never persist\"}]}}]"));
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var strategy = new GitHubActivitySnapshotStrategy(httpClient, tokenManagementService, repository, properties,
                tokenProperties, new ObjectMapper());
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());

        var request = ArgumentCaptor.forClass(com.samsepiol.library.http.request.ApiRequest.class);
        verify(httpClient).executeWithResponse(request.capture());
        assertThat(request.getValue().getHeaders()).containsEntry("If-None-Match", "\"prior\"")
                .containsEntry("Authorization", "Bearer token-not-to-log");
        var replacement = ArgumentCaptor.forClass(ExternalSnapshotEntity.class);
        verify(repository).replace(replacement.capture());
        assertThat(replacement.getValue().getProviderEtag()).isEqualTo("\"next\"");
        assertThat(replacement.getValue().getContent().get("events"))
                .contains("PushEvent", "2026-08-23", "owner/public-repo")
                .doesNotContain("never persist", "payload");
        assertThat(result.getSnapshot().getContent()).isEqualTo(replacement.getValue().getContent());
    }

    @Test
    void leavesTheLastKnownGoodSnapshotUntouchedOnNotModified() {
        var httpClient = mock(HttpClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubActivitySnapshotRepository.class);
        var properties = new GitHubRefreshProperties(true, true, "github-primary", "octocat", "0 */15 * * * *");
        var tokenProperties = new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_ACTIVITY).profileId("github-primary")
                .state(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY).title("Recent public activity")
                .sourceLabel("GitHub").refreshedAt(java.time.Instant.EPOCH).validUntil(java.time.Instant.EPOCH)
                .content(Map.of("events", "[]")).publicApproved(true).profileEnabled(true).providerEtag("\"prior\"").build();
        when(repository.find("github-primary")).thenReturn(Optional.of(existing));
        when(httpClient.executeWithResponse(any())).thenReturn(new HttpResponseEnvelope(304, Map.of("ETag", List.of("\"prior\"")), ""));
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var strategy = new GitHubActivitySnapshotStrategy(httpClient, tokenManagementService, repository, properties,
                tokenProperties, new ObjectMapper());
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).replace(any());
        assertThat(result.getSnapshot().getContent()).isEqualTo(Map.of("events", "[]"));
    }
}
