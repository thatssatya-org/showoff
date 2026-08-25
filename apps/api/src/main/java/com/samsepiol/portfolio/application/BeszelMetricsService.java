package com.samsepiol.portfolio.application;

import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.portfolio.api.BeszelMetricsResponse;
import com.samsepiol.portfolio.configuration.BeszelProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.provider.beszel.BeszelMetricsRequest;
import com.samsepiol.portfolio.provider.beszel.BeszelMetricSystem;
import com.samsepiol.portfolio.provider.beszel.BeszelProviderException;
import com.samsepiol.portfolio.provider.beszel.BeszelRestClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "portfolio.beszel", name = "enabled", havingValue = "true")
public class BeszelMetricsService {
    private static final TokenReference BESZEL_TOKEN_REFERENCE = new TokenReference("portfolio", "beszel", "api-token");

    private final TokenManagementService tokenManagementService;
    private final BeszelRestClient beszelRestClient;
    private final BeszelProperties properties;
    private final GitHubTokenProperties tokenProperties;

    private volatile CachedMetrics cachedMetrics;

    public BeszelMetricsResponse metrics() {
        var existing = cachedMetrics;
        if (isFresh(existing)) {
            return existing.response();
        }
        synchronized (this) {
            existing = cachedMetrics;
            if (isFresh(existing)) {
                return existing.response();
            }
            try {
                var systems = tokenManagementService.useForInternalIntegration(storageContext(), authorization(),
                        token -> beszelRestClient.fetchMetrics(new BeszelMetricsRequest(token)));
                var response = BeszelMetricsResponse.builder().refreshedAt(Instant.now()).stale(false).systems(systems).build();
                cachedMetrics = new CachedMetrics(response);
                return response;
            } catch (BeszelProviderException exception) {
                if (existing == null) {
                    throw exception;
                }
                return BeszelMetricsResponse.builder().refreshedAt(existing.response().getRefreshedAt()).stale(true)
                        .systems(existing.response().getSystems()).build();
            }
        }
    }

    private boolean isFresh(CachedMetrics value) {
        return value != null && value.response().getRefreshedAt().plus(properties.cacheTtl()).isAfter(Instant.now());
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(BESZEL_TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }

    private ManagementAuthorizationRequest authorization() {
        return ManagementAuthorizationRequest.builder().principalId("beszel-metrics-reader")
                .operation(GitHubTokenManagementConfiguration.BESZEL_TOKEN_USE_OPERATION).attributes(java.util.Map.of()).build();
    }

    private record CachedMetrics(BeszelMetricsResponse response) {
    }
}
