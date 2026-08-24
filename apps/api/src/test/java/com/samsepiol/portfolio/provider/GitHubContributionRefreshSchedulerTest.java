package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.domain.CapabilityType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GitHubContributionRefreshSchedulerTest {
    @Test
    void refreshesContributionsWhenTheApplicationBecomesReady() {
        var strategy = mock(GitHubContributionSnapshotStrategy.class);
        var scheduler = new GitHubContributionRefreshScheduler(strategy);

        scheduler.refreshOnStartup();

        var request = ArgumentCaptor.forClass(CapabilitySnapshotRefreshRequest.class);
        verify(strategy).refresh(request.capture());
        assertThat(request.getValue().getCapability()).isEqualTo(CapabilityType.GITHUB_CONTRIBUTIONS);
    }
}
