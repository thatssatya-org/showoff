package com.samsepiol.portfolio.provider;

import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenUse;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.repository.GitHubActivitySnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import com.samsepiol.portfolio.provider.github.GithubServiceClient;
import com.samsepiol.portfolio.provider.github.GitHubActivityFetchResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitHubActivitySnapshotStrategyTest {
    @Test
    void sendsTheStoredEtagAndAtomicallyReplacesOnlyThePublicEventProjection() {
        var githubServiceClient = mock(GithubServiceClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubActivitySnapshotRepository.class);
        var properties = refreshProperties();
        var tokenProperties = new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_ACTIVITY).profileId("github-primary")
                .state(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY).title("Recent public activity")
                .sourceLabel("GitHub").refreshedAt(java.time.Instant.EPOCH).validUntil(java.time.Instant.EPOCH)
                .content(Map.of("events", "[]")).publicApproved(true).profileEnabled(true).providerEtag("\"prior\"").build();
        when(repository.find("github-primary")).thenReturn(existing);
        var response = GitHubActivityFetchResponse.builder().statusCode(200).etag("\"next\"").events(List.of()).build();
        when(githubServiceClient.fetchPublicEvents(any(), eq("\"prior\""))).thenReturn(response);
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var strategy = new GitHubActivitySnapshotStrategy(githubServiceClient, tokenManagementService, repository, properties,
                tokenProperties);
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());

        verify(githubServiceClient).fetchPublicEvents(any(), eq("\"prior\""));
        var replacement = ArgumentCaptor.forClass(ExternalSnapshotEntity.class);
        verify(repository).replace(replacement.capture());
        assertThat(replacement.getValue().getContent().get("events"))
                .isEqualTo("[]");
        assertThat(result.getSnapshot().getContent()).isEqualTo(replacement.getValue().getContent());
    }

    @Test
    void leavesTheLastKnownGoodSnapshotUntouchedOnNotModified() {
        var githubServiceClient = mock(GithubServiceClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubActivitySnapshotRepository.class);
        var properties = refreshProperties();
        var tokenProperties = new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_ACTIVITY).profileId("github-primary")
                .state(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY).title("Recent public activity")
                .sourceLabel("GitHub").refreshedAt(java.time.Instant.EPOCH).validUntil(java.time.Instant.EPOCH)
                .content(Map.of("events", "[]")).publicApproved(true).profileEnabled(true).providerEtag("\"prior\"").build();
        when(repository.find("github-primary")).thenReturn(existing);
        when(githubServiceClient.fetchPublicEvents(any(), eq("\"prior\"")))
                .thenReturn(GitHubActivityFetchResponse.builder().statusCode(304).etag("\"prior\"").events(List.of()).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());
        var strategy = new GitHubActivitySnapshotStrategy(githubServiceClient, tokenManagementService, repository, properties,
                tokenProperties);
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).replace(any());
        assertThat(result.getSnapshot().getContent()).isEqualTo(Map.of("events", "[]"));
    }

    private static GitHubRefreshProperties refreshProperties() {
        var properties = new GitHubRefreshProperties();
        properties.setEnabled(true);
        properties.setPublicApproved(true);
        properties.setProfileId("github-primary");
        properties.setHandle("octocat");
        properties.setCron("0 */15 * * * *");
        return properties;
    }
}
