package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.provider.github.GitHubContributionCalendarResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionDayResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionWeekResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubContributionSnapshotMapperTest {
    private final GitHubContributionSnapshotMapper mapper = GitHubContributionSnapshotMapper.INSTANCE;

    @Test
    void persistsOnlyDayCountTotalAndThePrivateContributionDisclosureLabel() {
        var week = GitHubContributionWeekResponse.builder().contributionDays(List.of(
                GitHubContributionDayResponse.builder().date("2026-08-22").contributionCount(0).build(),
                GitHubContributionDayResponse.builder().date("2026-08-23").contributionCount(9).build()))
                .build();
        var calendar = GitHubContributionCalendarResponse.builder().totalContributions(9)
                .weeks(List.of(week))
                .build();
        var response = GitHubContributionFetchResponse.builder().statusCode(200).contributionCalendar(calendar).build();

        var entity = mapper.toEntity(response, refreshProperties(), Instant.EPOCH, Instant.EPOCH.plusSeconds(86_400));

        assertThat(entity.getCapability()).isEqualTo(CapabilityType.GITHUB_CONTRIBUTIONS);
        assertThat(entity.getContent()).containsOnlyKeys("totalContributions", "includesPrivateContributions", "contributionDays");
        assertThat(entity.getContent()).containsEntry("totalContributions", "9")
                .containsEntry("includesPrivateContributions", "true");
        assertThat(entity.getContent().get("contributionDays"))
                .contains("2026-08-22", "2026-08-23", "count")
                .doesNotContain("repository", "commit", "issue", "pullRequest", "private");
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
