package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.domain.CapabilityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = {"enabled", "private-contribution-disclosure-approved"},
        havingValue = "true")
@RequiredArgsConstructor
public class GitHubContributionRefreshScheduler {
    private final GitHubContributionSnapshotStrategy strategy;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh();
    }

    @Scheduled(cron = "${portfolio.github-refresh.contribution-cron}")
    public void refresh() {
        try {
            strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_CONTRIBUTIONS).build());
        } catch (RuntimeException exception) {
            log.warn("GitHub contribution refresh could not start; no visitor request is affected", exception);
        }
    }
}
