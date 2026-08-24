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
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = {"enabled", "public-approved"}, havingValue = "true")
@RequiredArgsConstructor
public class GitHubRepositoryRefreshScheduler {
    private final GitHubRepositorySnapshotStrategy strategy;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh();
    }

    @Scheduled(cron = "${portfolio.github-refresh.repository-cron}")
    public void refresh() {
        try {
            strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_REPOSITORIES).build());
        } catch (RuntimeException exception) {
            log.warn("GitHub repository refresh could not start; no visitor request is affected", exception);
        }
    }
}
