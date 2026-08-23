package com.samsepiol.portfolio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.constants.HttpConstants;
import com.samsepiol.library.http.request.ApiRequest;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.portfolio.configuration.GitHubRefreshConfiguration;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.repository.GitHubActivitySnapshotRepository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = "enabled", havingValue = "true")
public final class GitHubActivitySnapshotStrategy implements CapabilitySnapshotStrategy {
    private static final TokenReference TOKEN_REFERENCE = new TokenReference("portfolio", "github", "personal-access-token");
    private static final ManagementAuthorizationRequest INTERNAL_AUTHORIZATION = ManagementAuthorizationRequest.builder()
            .principalId("github-refresh-scheduler")
            .operation(GitHubTokenManagementConfiguration.GITHUB_TOKEN_USE_OPERATION)
            .build();
    private static final int MAX_EVENTS = 8;

    private final HttpClient httpClient;
    private final TokenManagementService tokenManagementService;
    private final GitHubActivitySnapshotRepository snapshotRepository;
    private final GitHubRefreshProperties refreshProperties;
    private final GitHubTokenProperties tokenProperties;
    private final ObjectMapper objectMapper;

    public GitHubActivitySnapshotStrategy(@Qualifier("gitHubHttpClient") HttpClient httpClient, TokenManagementService tokenManagementService,
                                          GitHubActivitySnapshotRepository snapshotRepository,
                                          GitHubRefreshProperties refreshProperties, GitHubTokenProperties tokenProperties,
                                          ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.tokenManagementService = tokenManagementService;
        this.snapshotRepository = snapshotRepository;
        this.refreshProperties = refreshProperties;
        this.tokenProperties = tokenProperties;
        this.objectMapper = objectMapper;
    }

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
        var headers = new LinkedHashMap<String, String>();
        headers.put(HttpConstants.Headers.AUTHORIZATION, "Bearer " + new String(token));
        headers.put("Accept", "application/vnd.github+json");
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        headers.put("User-Agent", "showoff-github-refresh");
        existing.map(ExternalSnapshotEntity::getProviderEtag).filter(value -> !value.isBlank())
                .ifPresent(value -> headers.put("If-None-Match", value));
        try {
            var response = httpClient.executeWithResponse(ApiRequest.builder()
                    .service(GitHubRefreshConfiguration.SERVICE)
                    .api(GitHubRefreshConfiguration.PUBLIC_EVENTS)
                    .headers(Map.copyOf(headers))
                    .build());
            if (response.getStatusCode() == 304) {
                return existing.map(this::toPublicSnapshot).orElseGet(this::emptySnapshot);
            }
            if (!response.isSuccessful()) {
                throw new IllegalStateException("GitHub returned an unsuccessful response");
            }
            var replacement = toEntity(response.getBody(), response.firstHeader("etag").orElse(null));
            snapshotRepository.replace(replacement);
            return toPublicSnapshot(replacement);
        } catch (RuntimeException exception) {
            log.warn("GitHub activity refresh failed; retaining the last known good snapshot");
        }
        return existing.map(this::toPublicSnapshot).orElseGet(this::emptySnapshot);
    }

    private ExternalSnapshotEntity toEntity(String body, String etag) {
        var now = Instant.now();
        return ExternalSnapshotEntity.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .profileId(refreshProperties.profileId())
                .state(CapabilityState.HEALTHY)
                .title("Recent public activity")
                .sourceLabel("GitHub")
                .refreshedAt(now)
                .validUntil(now.plusSeconds(900))
                .content(Map.of("events", serializePublicEvents(body)))
                .publicApproved(refreshProperties.publicApproved())
                .profileEnabled(true)
                .providerEtag(etag)
                .build();
    }

    private String serializePublicEvents(String body) {
        try {
            var root = objectMapper.readTree(body);
            if (!root.isArray()) {
                throw new IllegalArgumentException("GitHub events response must be an array");
            }
            var events = new ArrayList<GitHubPublicEvent>();
            for (JsonNode node : root) {
                if (events.size() == MAX_EVENTS) {
                    break;
                }
                var type = node.path("type").asText();
                var repository = node.path("repo").path("name").asText();
                var createdAt = node.path("created_at").asText();
                if (!type.isBlank() && !repository.isBlank() && !createdAt.isBlank()) {
                    var day = Instant.parse(createdAt).atZone(ZoneOffset.UTC).toLocalDate().toString();
                    events.add(GitHubPublicEvent.builder().type(type).repository(repository).day(day).build());
                }
            }
            return objectMapper.writeValueAsString(List.copyOf(events));
        } catch (Exception exception) {
            throw new IllegalArgumentException("GitHub events response cannot be mapped to the public schema", exception);
        }
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }

    private PublicCapabilitySnapshot toPublicSnapshot(ExternalSnapshotEntity entity) {
        return PublicCapabilitySnapshot.builder().capability(entity.getCapability()).state(entity.getState())
                .title(entity.getTitle()).sourceLabel(entity.getSourceLabel()).refreshedAt(entity.getRefreshedAt())
                .content(entity.getContent()).build();
    }

    private PublicCapabilitySnapshot emptySnapshot() {
        return PublicCapabilitySnapshot.builder().capability(CapabilityType.GITHUB_ACTIVITY)
                .state(CapabilityState.AWAITING_AUTHORIZATION).title("Recent public activity").sourceLabel("GitHub")
                .refreshedAt(Instant.EPOCH).content(Map.of()).build();
    }
}
