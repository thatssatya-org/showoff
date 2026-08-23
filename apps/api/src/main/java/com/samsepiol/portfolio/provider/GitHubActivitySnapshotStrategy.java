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
            var eventsResponse = httpClient.executeWithResponse(ApiRequest.builder()
                    .service(GitHubRefreshConfiguration.SERVICE)
                    .api(GitHubRefreshConfiguration.PUBLIC_EVENTS)
                    .headers(Map.copyOf(headers))
                    .build(), String.class);
            var events = eventsResponse.getStatusCode() == 304
                    ? existing.map(snapshot -> snapshot.getContent().get("events")).orElse(null)
                    : publicEvents(eventsResponse.getBody());
            if (events == null || !eventsResponse.isSuccessful() && eventsResponse.getStatusCode() != 304) {
                throw new IllegalStateException("GitHub events returned an unsuccessful response");
            }
            var contributionsResponse = httpClient.executeWithResponse(ApiRequest.builder()
                    .service(GitHubRefreshConfiguration.SERVICE)
                    .api(GitHubRefreshConfiguration.CONTRIBUTIONS)
                    .headers(graphQlHeaders(token))
                    .body(Map.of("query", "query { viewer { contributionsCollection { contributionCalendar { totalContributions weeks { contributionDays { date contributionCount } } } } } }"))
                    .build(), JsonNode.class);
            var repositoryResponse = httpClient.executeWithResponse(ApiRequest.builder()
                    .service(GitHubRefreshConfiguration.SERVICE)
                    .api(GitHubRefreshConfiguration.REPOSITORY)
                    .headers(Map.copyOf(headers))
                    .build(), String.class);
            if (!contributionsResponse.isSuccessful() || !repositoryResponse.isSuccessful()) {
                throw new IllegalStateException("GitHub public projection returned an unsuccessful response");
            }
            var replacement = toEntity(events, contributionCalendar(contributionsResponse.getBody()),
                    repository(repositoryResponse.getBody()), eventsResponse.firstHeader("etag").orElse(null));
            snapshotRepository.replace(replacement);
            return toPublicSnapshot(replacement);
        } catch (RuntimeException exception) {
            log.warn("GitHub activity refresh failed; retaining the last known good snapshot");
        }
        return existing.map(this::toPublicSnapshot).orElseGet(this::emptySnapshot);
    }

    private Map<String, String> graphQlHeaders(char[] token) {
        var headers = new LinkedHashMap<String, String>();
        headers.put(HttpConstants.Headers.AUTHORIZATION, "Bearer " + new String(token));
        headers.put("Accept", "application/vnd.github+json");
        headers.put("User-Agent", "showoff-github-refresh");
        return Map.copyOf(headers);
    }

    private ExternalSnapshotEntity toEntity(Object events, Map<String, Object> contributions,
                                            Map<String, Object> repository, String etag) {
        var now = Instant.now();
        return ExternalSnapshotEntity.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .profileId(refreshProperties.profileId())
                .state(CapabilityState.HEALTHY)
                .title("Recent public activity")
                .sourceLabel("GitHub")
                .refreshedAt(now)
                .validUntil(now.plusSeconds(900))
                .content(Map.of("events", events, "contributions", contributions, "repositories", List.of(repository)))
                .publicApproved(refreshProperties.publicApproved())
                .profileEnabled(true)
                .providerEtag(etag)
                .build();
    }

    private List<Map<String, String>> publicEvents(String body) {
        try {
            var root = objectMapper.readTree(body);
            if (!root.isArray()) {
                throw new IllegalArgumentException("GitHub events response must be an array");
            }
            var events = new ArrayList<Map<String, String>>();
            for (JsonNode node : root) {
                if (events.size() == MAX_EVENTS) {
                    break;
                }
                var type = node.path("type").asText();
                var repository = node.path("repo").path("name").asText();
                var createdAt = node.path("created_at").asText();
                if (!type.isBlank() && !repository.isBlank() && !createdAt.isBlank()) {
                    var day = Instant.parse(createdAt).atZone(ZoneOffset.UTC).toLocalDate().toString();
                    events.add(Map.of("type", type, "repository", repository, "day", day));
                }
            }
            return List.copyOf(events);
        } catch (Exception exception) {
            throw new IllegalArgumentException("GitHub events response cannot be mapped to the public schema", exception);
        }
    }

    private Map<String, Object> contributionCalendar(JsonNode response) {
        var calendar = response.path("data").path("viewer").path("contributionsCollection").path("contributionCalendar");
        if (calendar.isMissingNode() || !calendar.path("totalContributions").canConvertToInt()) {
            throw new IllegalArgumentException("GitHub contribution calendar cannot be mapped to the public schema");
        }
        var days = new ArrayList<Map<String, Object>>();
        for (JsonNode week : calendar.path("weeks")) {
            for (JsonNode day : week.path("contributionDays")) {
                var date = day.path("date").asText();
                if (date.isBlank() || !day.path("contributionCount").canConvertToInt()) {
                    throw new IllegalArgumentException("GitHub contribution day cannot be mapped to the public schema");
                }
                days.add(Map.of("date", date, "count", day.path("contributionCount").asInt()));
            }
        }
        return Map.of("total", calendar.path("totalContributions").asInt(), "days", List.copyOf(days));
    }

    private Map<String, Object> repository(String body) {
        try {
            var root = objectMapper.readTree(body);
            var name = root.path("name").asText();
            var url = root.path("html_url").asText();
            if (name.isBlank() || url.isBlank() || !root.path("stargazers_count").canConvertToInt()) {
                throw new IllegalArgumentException("GitHub repository cannot be mapped to the public schema");
            }
            var topics = new ArrayList<String>();
            for (JsonNode topic : root.path("topics")) {
                if (topic.isTextual() && !topic.asText().isBlank()) {
                    topics.add(topic.asText());
                }
            }
            var result = new LinkedHashMap<String, Object>();
            result.put("name", name);
            result.put("url", url);
            result.put("stars", root.path("stargazers_count").asInt());
            result.put("topics", List.copyOf(topics));
            if (root.path("language").isTextual()) result.put("primaryLanguage", root.path("language").asText());
            if (root.path("updated_at").isTextual()) result.put("updatedAt", root.path("updated_at").asText());
            if (root.path("description").isTextual()) result.put("description", root.path("description").asText());
            return Map.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalArgumentException("GitHub repository cannot be mapped to the public schema", exception);
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
