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
import com.samsepiol.portfolio.provider.github.GitHubRepositoryRequest;
import com.samsepiol.portfolio.provider.github.GithubServiceClient;
import com.samsepiol.portfolio.repository.GitHubRepositorySnapshotRepository;
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
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = {"enabled", "public-approved"}, havingValue = "true")
@RequiredArgsConstructor
public final class GitHubRepositorySnapshotStrategy implements CapabilitySnapshotStrategy {
    private static final TokenReference TOKEN_REFERENCE = new TokenReference("portfolio", "github", "personal-access-token");
    private static final ManagementAuthorizationRequest INTERNAL_AUTHORIZATION = ManagementAuthorizationRequest.builder()
            .principalId("github-refresh-scheduler")
            .operation(GitHubTokenManagementConfiguration.GITHUB_TOKEN_USE_OPERATION)
            .build();
    private final GithubServiceClient githubServiceClient;
    private final TokenManagementService tokenManagementService;
    private final GitHubRepositorySnapshotRepository snapshotRepository;
    private final GitHubRefreshProperties refreshProperties;
    private final GitHubTokenProperties tokenProperties;

    @Override
    public @NonNull CapabilityType capabilityType() {
        return CapabilityType.GITHUB_REPOSITORIES;
    }

    @Override
    public @NonNull CapabilitySnapshotRefreshResponse refresh(@NonNull CapabilitySnapshotRefreshRequest request) {
        var existing = snapshotRepository.find(refreshProperties.getProfileId());
        if (isFresh(existing)) {
            return CapabilitySnapshotRefreshResponse.builder()
                    .snapshot(GitHubRepositorySnapshotMapper.INSTANCE.toPublicSnapshot(existing)).build();
        }
        var snapshot = tokenManagementService.useForInternalIntegration(storageContext(), INTERNAL_AUTHORIZATION,
                token -> refreshWithToken(token, existing));
        return CapabilitySnapshotRefreshResponse.builder().snapshot(snapshot).build();
    }

    private PublicCapabilitySnapshot refreshWithToken(char[] token, ExternalSnapshotEntity existing) {
        try {
            var response = githubServiceClient.fetchRepository(token, repositoryRequest());
            if (!response.isSuccessful()) {
                throw new GitHubProviderException(GitHubProviderError.UNSUCCESSFUL_REPOSITORY_RESPONSE);
            }
            var refreshedAt = Instant.ofEpochMilli(DateTimeUtils.currentEpochMillis());
            var replacement = GitHubRepositorySnapshotMapper.INSTANCE.toEntity(response, refreshProperties, refreshedAt,
                    refreshedAt.plus(1, ChronoUnit.HOURS));
            snapshotRepository.replace(replacement);
            return GitHubRepositorySnapshotMapper.INSTANCE.toPublicSnapshot(replacement);
        } catch (RuntimeException exception) {
            log.warn("GitHub repository refresh failed; retaining the last known good snapshot", exception);
        }
        return existing == null ? emptySnapshot() : GitHubRepositorySnapshotMapper.INSTANCE.toPublicSnapshot(existing);
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }

    private boolean isFresh(ExternalSnapshotEntity snapshot) {
        return snapshot != null && snapshot.getValidUntilEpochMillis() > DateTimeUtils.currentEpochMillis();
    }

    private GitHubRepositoryRequest repositoryRequest() {
        return GitHubRepositoryRequest.builder().query(GitHubRepositoryRequest.QUERY)
                .variables(Map.of("owner", refreshProperties.getRepositoryOwner(), "name", refreshProperties.getRepositoryName()))
                .expectedOwner(refreshProperties.getRepositoryOwner()).expectedName(refreshProperties.getRepositoryName()).build();
    }

    private PublicCapabilitySnapshot emptySnapshot() {
        return PublicCapabilitySnapshot.builder().capability(CapabilityType.GITHUB_REPOSITORIES)
                .state(CapabilityState.AWAITING_AUTHORIZATION).title("Featured GitHub repository").sourceLabel("GitHub")
                .refreshedAt(Instant.EPOCH).content(Map.of()).build();
    }
}
