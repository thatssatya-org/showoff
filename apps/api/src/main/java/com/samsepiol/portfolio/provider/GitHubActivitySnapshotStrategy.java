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
import com.samsepiol.portfolio.provider.github.GitHubActivityClient;
import com.samsepiol.portfolio.repository.GitHubActivitySnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public final class GitHubActivitySnapshotStrategy implements CapabilitySnapshotStrategy {
    private static final TokenReference TOKEN_REFERENCE = new TokenReference("portfolio", "github", "personal-access-token");
    private static final ManagementAuthorizationRequest INTERNAL_AUTHORIZATION = ManagementAuthorizationRequest.builder()
            .principalId("github-refresh-scheduler")
            .operation(GitHubTokenManagementConfiguration.GITHUB_TOKEN_USE_OPERATION)
            .build();
    private final GitHubActivityClient gitHubActivityClient;
    private final TokenManagementService tokenManagementService;
    private final GitHubActivitySnapshotRepository snapshotRepository;
    private final GitHubRefreshProperties refreshProperties;
    private final GitHubTokenProperties tokenProperties;
    private final GitHubActivitySnapshotMapper snapshotMapper;

    @Override
    public @NonNull CapabilityType capabilityType() {
        return CapabilityType.GITHUB_ACTIVITY;
    }

    @Override
    public @NonNull CapabilitySnapshotRefreshResponse refresh(@NonNull CapabilitySnapshotRefreshRequest request) {
        if (request.getCapability() != CapabilityType.GITHUB_ACTIVITY) {
            throw new IllegalArgumentException("GitHub activity strategy accepts only GITHUB_ACTIVITY");
        }
        var existing = snapshotRepository.find(refreshProperties.profileId());
        var snapshot = tokenManagementService.useForInternalIntegration(storageContext(), INTERNAL_AUTHORIZATION,
                token -> refreshWithToken(token, existing));
        return CapabilitySnapshotRefreshResponse.builder().snapshot(snapshot).build();
    }

    private PublicCapabilitySnapshot refreshWithToken(char[] token, java.util.Optional<ExternalSnapshotEntity> existing) {
        try {
            var response = gitHubActivityClient.fetchPublicEvents(token,
                    existing.map(ExternalSnapshotEntity::getProviderEtag).orElse(null));
            if (response.isNotModified()) {
                return existing.map(snapshotMapper::toPublicSnapshot).orElseGet(this::emptySnapshot);
            }
            if (!response.isSuccessful()) {
                throw new IllegalStateException("GitHub returned an unsuccessful response");
            }
            var refreshedAt = Instant.ofEpochMilli(DateTimeUtils.currentEpochMillis());
            var replacement = snapshotMapper.toEntity(response, refreshProperties, refreshedAt, refreshedAt.plusSeconds(900));
            snapshotRepository.replace(replacement);
            return snapshotMapper.toPublicSnapshot(replacement);
        } catch (RuntimeException exception) {
            log.warn("GitHub activity refresh failed; retaining the last known good snapshot");
        }
        return existing.map(snapshotMapper::toPublicSnapshot).orElseGet(this::emptySnapshot);
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }

    private PublicCapabilitySnapshot emptySnapshot() {
        return PublicCapabilitySnapshot.builder().capability(CapabilityType.GITHUB_ACTIVITY)
                .state(CapabilityState.AWAITING_AUTHORIZATION).title("Recent public activity").sourceLabel("GitHub")
                .refreshedAt(Instant.EPOCH).content(Map.of()).build();
    }
}
