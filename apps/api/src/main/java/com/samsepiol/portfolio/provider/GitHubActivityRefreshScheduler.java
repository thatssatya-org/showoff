package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.domain.CapabilityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class GitHubActivityRefreshScheduler {
    private final GitHubActivitySnapshotStrategy strategy;

    @Scheduled(cron = "${portfolio.github-refresh.cron}")
    public void refresh() {
        try {
            strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());
        } catch (RuntimeException exception) {
            log.warn("GitHub activity refresh could not start; no visitor request is affected", exception);
        }
    }
}
