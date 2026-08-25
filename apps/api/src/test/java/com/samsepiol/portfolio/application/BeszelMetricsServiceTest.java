package com.samsepiol.portfolio.application;

import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenUse;
import com.samsepiol.portfolio.configuration.BeszelProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.provider.beszel.BeszelMetricSystem;
import com.samsepiol.portfolio.provider.beszel.BeszelRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeszelMetricsServiceTest {
    @Mock
    private TokenManagementService tokenManagementService;
    @Mock
    private BeszelRestClient beszelRestClient;

    @Test
    void returnsFreshCacheWithoutDecryptingOrCallingBeszelAgain() {
        var service = service(Duration.ofMinutes(1));
        when(tokenManagementService.useForInternalIntegration(any(), any(), any())).thenAnswer(invocation ->
                invocation.<TokenUse<List<BeszelMetricSystem>>>getArgument(2).use("token".toCharArray()));
        when(beszelRestClient.fetchMetrics(any())).thenReturn(List.of(system("first")));

        var initial = service.metrics();
        var cached = service.metrics();

        assertThat(cached).isEqualTo(initial);
        verify(tokenManagementService).useForInternalIntegration(any(), any(), any());
        verify(beszelRestClient).fetchMetrics(any());
    }

    @Test
    void refreshesWhenTheCacheIsStale() {
        var service = service(Duration.ZERO);
        when(tokenManagementService.useForInternalIntegration(any(), any(), any())).thenAnswer(invocation ->
                invocation.<TokenUse<List<BeszelMetricSystem>>>getArgument(2).use("token".toCharArray()));
        when(beszelRestClient.fetchMetrics(any())).thenReturn(List.of(system("first")), List.of(system("second")));

        var initial = service.metrics();
        var refreshed = service.metrics();

        assertThat(initial.getSystems()).containsExactly(system("first"));
        assertThat(refreshed.getSystems()).containsExactly(system("second"));
        verify(tokenManagementService, org.mockito.Mockito.times(2)).useForInternalIntegration(any(), any(), any());
        verify(beszelRestClient, org.mockito.Mockito.times(2)).fetchMetrics(any());
    }

    private BeszelMetricsService service(Duration cacheTtl) {
        return new BeszelMetricsService(tokenManagementService, beszelRestClient,
                new BeszelProperties(true, "https://beszel.tailnet.ts.net", cacheTtl),
                new GitHubTokenProperties("key-id", "unused", List.of("172.30.0.0/24"), List.of("100.64.0.0/10")));
    }

    private static BeszelMetricSystem system(String name) {
        return new BeszelMetricSystem(name, "up", null, 1.0, 2.0, 3.0, 0.1);
    }
}
