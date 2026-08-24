package com.samsepiol.portfolio.provider;

import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenUse;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.provider.github.GitHubContributionCalendarResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionDayResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionWeekResponse;
import com.samsepiol.portfolio.provider.github.GithubServiceClient;
import com.samsepiol.portfolio.repository.GitHubContributionSnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitHubContributionSnapshotStrategyTest {
    @Test
    void atomicallyPersistsOnlyTheApprovedContributionProjection() {
        var githubServiceClient = mock(GithubServiceClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubContributionSnapshotRepository.class);
        var week = GitHubContributionWeekResponse.builder().contributionDays(List.of(
                GitHubContributionDayResponse.builder().date("2026-08-23").contributionCount(7).build()))
                .build();
        var calendar = GitHubContributionCalendarResponse.builder().totalContributions(7)
                .weeks(List.of(week))
                .build();
        when(githubServiceClient.fetchContributionCalendar(any(), any(), any(), any()))
                .thenReturn(GitHubContributionFetchResponse.builder().statusCode(200).contributionCalendar(calendar).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var strategy = strategy(githubServiceClient, tokenManagementService, repository);
        var result = strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_CONTRIBUTIONS).build());

        var replacement = ArgumentCaptor.forClass(ExternalSnapshotEntity.class);
        verify(repository).replace(replacement.capture());
        assertThat(replacement.getValue().getContent()).containsOnlyKeys("totalContributions", "includesPrivateContributions",
                "contributionDays");
        assertThat(result.getSnapshot().getCapability()).isEqualTo(CapabilityType.GITHUB_CONTRIBUTIONS);
    }

    @Test
    void retainsTheLastKnownGoodSnapshotOnAGraphQlFailure() {
        var githubServiceClient = mock(GithubServiceClient.class);
        var tokenManagementService = mock(TokenManagementService.class);
        var repository = mock(GitHubContributionSnapshotRepository.class);
        var existing = ExternalSnapshotEntity.builder().capability(CapabilityType.GITHUB_CONTRIBUTIONS).profileId("github-primary")
                .state(CapabilityState.HEALTHY).title("GitHub contributions").sourceLabel("GitHub")
                .refreshedAtEpochMillis(0L).validUntilEpochMillis(86_400_000L)
                .content(Map.of("totalContributions", "1", "includesPrivateContributions", "true",
                        "contributionDays", "[{\"date\":\"2026-08-23\",\"count\":1}]"))
                .publicApproved(true).profileEnabled(true).build();
        when(repository.find("github-primary")).thenReturn(existing);
        when(githubServiceClient.fetchContributionCalendar(any(), any(), any(), any()))
                .thenReturn(GitHubContributionFetchResponse.builder().statusCode(200).hasErrors(true).build());
        doAnswer(invocation -> ((TokenUse<?>) invocation.getArgument(2)).use("token-not-to-log".toCharArray()))
                .when(tokenManagementService).useForInternalIntegration(any(), any(), any());

        var result = strategy(githubServiceClient, tokenManagementService, repository)
                .refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_CONTRIBUTIONS).build());

        verify(repository, never()).replace(any());
        assertThat(result.getSnapshot().getContent()).isEqualTo(existing.getContent());
    }

    private static GitHubContributionSnapshotStrategy strategy(GithubServiceClient githubServiceClient,
                                                                TokenManagementService tokenManagementService,
                                                                GitHubContributionSnapshotRepository repository) {
        return new GitHubContributionSnapshotStrategy(githubServiceClient, tokenManagementService, repository,
                refreshProperties(), new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                List.of("172.30.0.0/24"), List.of("100.64.0.0/10")));
    }

    private static GitHubRefreshProperties refreshProperties() {
        var properties = new GitHubRefreshProperties();
        properties.setEnabled(true);
        properties.setPublicApproved(true);
        properties.setProfileId("github-primary");
        properties.setHandle("octocat");
        properties.setCron("0 */15 * * * *");
        properties.setContributionCron("0 5 0 * * *");
        properties.setPrivateContributionDisclosureApproved(true);
        return properties;
    }
}
