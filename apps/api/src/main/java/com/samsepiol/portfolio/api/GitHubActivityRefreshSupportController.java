package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.provider.CapabilitySnapshotRefreshRequest;
import com.samsepiol.portfolio.provider.GitHubActivitySnapshotStrategy;
import com.samsepiol.portfolio.security.TailnetManagementAccess;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = "enabled", havingValue = "true")
@RequestMapping("/internal/v1/provider-profiles/github/activity")
public class GitHubActivityRefreshSupportController {
    private final GitHubActivitySnapshotStrategy strategy;
    private final TailnetManagementAccess tailnetManagementAccess;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest servletRequest) {
        tailnetManagementAccess.authorize(servletRequest);
        strategy.refresh(CapabilitySnapshotRefreshRequest.builder().capability(CapabilityType.GITHUB_ACTIVITY).build());
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
