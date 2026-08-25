package com.samsepiol.portfolio.provider.beszel;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.constants.HttpConstants;
import com.samsepiol.library.http.request.ApiRequest;
import com.samsepiol.portfolio.configuration.BeszelHttpConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Beszel's authenticated PocketBase collections are consumed server-side only.
 * Projection happens here so collection records and identifiers cannot leak to
 * the browser contract.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "portfolio.beszel", name = "enabled", havingValue = "true")
public final class DefaultBeszelRestClient implements BeszelRestClient {
    private final HttpClient httpClient;

    @Override
    public List<BeszelMetricSystem> fetchMetrics(BeszelMetricsRequest request) {
        try {
            var token = new String(request.token());
            var headers = Map.of(HttpConstants.Headers.AUTHORIZATION, "Bearer " + token);
            var systems = httpClient.execute(ApiRequest.builder().service(BeszelHttpConfiguration.SERVICE)
                    .api(BeszelHttpConfiguration.SYSTEMS_API).headers(headers).build(), BeszelRecordsResponse.class);
            var stats = httpClient.execute(ApiRequest.builder().service(BeszelHttpConfiguration.SERVICE)
                    .api(BeszelHttpConfiguration.LATEST_STATS_API).headers(headers).build(), BeszelStatsResponse.class);
            var latestStats = latestBySystem(stats == null ? List.of() : stats.items());
            return (systems == null ? List.<BeszelSystemResponse>of() : systems.items()).stream()
                    .map(system -> toMetricSystem(system, latestStats.get(system.id())))
                    .sorted(Comparator.comparing(BeszelMetricSystem::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (RuntimeException exception) {
            throw new BeszelProviderException(BeszelProviderError.UPSTREAM_UNAVAILABLE, exception);
        } finally {
            Arrays.fill(request.token(), '\0');
        }
    }

    private Map<String, BeszelStatsResponse.Record> latestBySystem(List<BeszelStatsResponse.Record> records) {
        var latest = new HashMap<String, BeszelStatsResponse.Record>();
        for (var record : records) {
            latest.putIfAbsent(record.system(), record);
        }
        return latest;
    }

    private BeszelMetricSystem toMetricSystem(BeszelSystemResponse system, BeszelStatsResponse.Record statsRecord) {
        var stats = statsRecord == null ? null : statsRecord.stats();
        return new BeszelMetricSystem(system.name(), system.status(),
                statsRecord == null ? null : statsRecord.created(),
                stats == null ? null : stats.cpu(), stats == null ? null : stats.memoryPercent(),
                stats == null ? null : stats.diskPercent(), stats == null ? null : stats.loadAverage());
    }

}
