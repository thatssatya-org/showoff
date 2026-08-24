package com.samsepiol.portfolio.provider;

import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.library.core.util.DateTimeUtils;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.provider.github.GithubServiceClient;
import com.samsepiol.portfolio.repository.GitHubContributionSnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = {"enabled", "private-contribution-disclosure-approved"},
        havingValue = "true")
@RequiredArgsConstructor
public final class GitHubContributionSnapshotStrategy implements CapabilitySnapshotStrategy {
    private static final TokenReference TOKEN_REFERENCE = new TokenReference("portfolio", "github", "personal-access-token");
    private static final ManagementAuthorizationRequest INTERNAL_AUTHORIZATION = ManagementAuthorizationRequest.builder()
            .principalId("github-refresh-scheduler")
            .operation(GitHubTokenManagementConfiguration.GITHUB_TOKEN_USE_OPERATION)
            .build();
    private final GithubServiceClient githubServiceClient;
    private final TokenManagementService tokenManagementService;
    private final GitHubContributionSnapshotRepository snapshotRepository;
    private final GitHubRefreshProperties refreshProperties;
    private final GitHubTokenProperties tokenProperties;

    @Override
    public @NonNull CapabilityType capabilityType() {
        return CapabilityType.GITHUB_CONTRIBUTIONS;
    }

    @Override
    public @NonNull CapabilitySnapshotRefreshResponse refresh(@NonNull CapabilitySnapshotRefreshRequest request) {
        if (request.getCapability() != CapabilityType.GITHUB_CONTRIBUTIONS) {
            throw new IllegalArgumentException("GitHub contribution strategy accepts only GITHUB_CONTRIBUTIONS");
        }
        var existing = snapshotRepository.find(refreshProperties.getProfileId());
        var snapshot = tokenManagementService.useForInternalIntegration(storageContext(), INTERNAL_AUTHORIZATION,
                token -> refreshWithToken(token, existing));
        return CapabilitySnapshotRefreshResponse.builder().snapshot(snapshot).build();
    }

    private PublicCapabilitySnapshot refreshWithToken(char[] token, ExternalSnapshotEntity existing) {
        try {
            var refreshedAt = Instant.ofEpochMilli(DateTimeUtils.currentEpochMillis());
            var response = githubServiceClient.fetchContributionCalendar(token, refreshProperties.getHandle(),
                    refreshedAt.minus(365, ChronoUnit.DAYS), refreshedAt);
            if (!response.isSuccessful()) {
                throw new IllegalStateException("GitHub contribution calendar response was unsuccessful");
            }
            var replacement = GitHubContributionSnapshotMapper.INSTANCE.toEntity(response, refreshProperties, refreshedAt,
                    refreshedAt.plus(1, ChronoUnit.DAYS));
            snapshotRepository.replace(replacement);
            return GitHubContributionSnapshotMapper.INSTANCE.toPublicSnapshot(replacement);
        } catch (RuntimeException exception) {
            log.warn("GitHub contribution refresh failed; retaining the last known good snapshot", exception);
        }
        return existing == null ? emptySnapshot() : GitHubContributionSnapshotMapper.INSTANCE.toPublicSnapshot(existing);
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }

    private PublicCapabilitySnapshot emptySnapshot() {
        return PublicCapabilitySnapshot.builder().capability(CapabilityType.GITHUB_CONTRIBUTIONS)
                .state(CapabilityState.AWAITING_AUTHORIZATION).title("GitHub contributions").sourceLabel("GitHub")
                .refreshedAt(Instant.EPOCH).content(Map.of()).build();
    }
}
