package com.samsepiol.portfolio.provider;

import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenUse;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryBranchResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryCommitResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryLanguageResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositorySnapshotResponse;
import com.samsepiol.portfolio.provider.github.GithubServiceClient;
import com.samsepiol.portfolio.repository.GitHubRepositorySnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubRepositorySnapshotStrategyTest {
    @Mock
    private GithubServiceClient githubServiceClient;
    @Mock
    private TokenManagementService tokenManagementService;
    @Mock
    private GitHubRepositorySnapshotRepository repository;
    @Spy
    private GitHubRefreshProperties refreshProperties = refreshProperties();
    @Spy
    private GitHubTokenProperties tokenProperties = new GitHubTokenProperties("github-token-v1",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
    @InjectMocks
    private GitHubRepositorySnapshotStrategy strategy;

    @Test
    void atomicallyPersistsOnlyTheApprovedPublicRepositoryProjection() {
        when(githubServiceClient.fetchRepository(any(), any()))
                .thenReturn(GitHubRepositoryFetchResponse.builder().statusCode(200).repository(publicRepository()).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_REPOSITORIES).build());

        var replacement = ArgumentCaptor.forClass(ExternalSnapshotEntity.class);
        verify(repository).replace(replacement.capture());
        assertThat(replacement.getValue().getContent()).containsOnlyKeys("repository", "url", "stars", "latestCommitDate", "language");
        assertThat(replacement.getValue().getContent()).doesNotContainKeys("isPrivate", "visibility", "defaultBranchRef");
        assertThat(result.getSnapshot().getCapability()).isEqualTo(CapabilityType.GITHUB_REPOSITORIES);
    }

    @Test
    void returnsFreshSnapshotWithoutAcquiringATokenOrCallingGitHub() {
        var existing = snapshot(Long.MAX_VALUE);
        when(repository.find("github-primary")).thenReturn(existing);

        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_REPOSITORIES).build());

        verify(tokenManagementService, never()).useForInternalIntegration(any(), any(), any());
        verify(githubServiceClient, never()).fetchRepository(any(), any());
        assertThat(result.getSnapshot().getContent()).isEqualTo(existing.getContent());
    }

    @Test
    void retainsTheLastKnownGoodSnapshotWhenGitHubRefusesThePublicRepositoryCheck() {
        var existing = snapshot(0L);
        when(repository.find("github-primary")).thenReturn(existing);
        when(githubServiceClient.fetchRepository(any(), any())).thenReturn(GitHubRepositoryFetchResponse.builder()
                .statusCode(200).repository(publicRepository()).hasErrors(true).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_REPOSITORIES).build());

        verify(repository, never()).replace(any());
        assertThat(result.getSnapshot().getContent()).isEqualTo(existing.getContent());
    }

    private static ExternalSnapshotEntity snapshot(long validUntilEpochMillis) {
        return ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_REPOSITORIES).profileId("github-primary")
                .state(CapabilityState.HEALTHY).title("Featured GitHub repository").sourceLabel("GitHub")
                .refreshedAtEpochMillis(0L).validUntilEpochMillis(validUntilEpochMillis)
                .content(Map.of("repository", "owner/easy-fintrack", "url", "https://github.com/owner/easy-fintrack",
                        "stars", "1", "latestCommitDate", "2026-08-24T00:00:00Z"))
                .publicApproved(true).profileEnabled(true).build();
    }

    private static GitHubRepositorySnapshotResponse publicRepository() {
        return GitHubRepositorySnapshotResponse.builder().nameWithOwner("owner/easy-fintrack").isPrivate(false)
                .visibility("PUBLIC").url("https://github.com/owner/easy-fintrack").stargazerCount(1)
                .primaryLanguage(GitHubRepositoryLanguageResponse.builder().name("Java").build())
                .defaultBranchRef(GitHubRepositoryBranchResponse.builder()
                        .target(GitHubRepositoryCommitResponse.builder().committedDate("2026-08-24T00:00:00Z").build()).build())
                .build();
    }

    private static GitHubRefreshProperties refreshProperties() {
        var properties = new GitHubRefreshProperties();
        properties.setEnabled(true);
        properties.setPublicApproved(true);
        properties.setProfileId("github-primary");
        properties.setHandle("octocat");
        properties.setCron("0 */15 * * * *");
        properties.setContributionCron("0 5 0 * * *");
        properties.setRepositoryCron("0 20 * * * *");
        properties.setRepositoryOwner("owner");
        properties.setRepositoryName("easy-fintrack");
        properties.setPrivateContributionDisclosureApproved(true);
        return properties;
    }
}
