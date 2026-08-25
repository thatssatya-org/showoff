package com.samsepiol.portfolio.provider.beszel;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.request.ApiRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBeszelRestClientTest {
    @Mock
    private HttpClient httpClient;
    @InjectMocks
    private DefaultBeszelRestClient beszelRestClient;

    @Test
    void fetchesOnlySystemAndLatestStatProjectionsWithTheStoredToken() {
        when(httpClient.execute(any(), eq(BeszelRecordsResponse.class))).thenReturn(new BeszelRecordsResponse(List.of(
                new BeszelSystemResponse("internal-system-id", "Satya WSL", "up"))));
        when(httpClient.execute(any(), eq(BeszelStatsResponse.class))).thenReturn(new BeszelStatsResponse(List.of(
                new BeszelStatsResponse.Record("internal-system-id",
                        new BeszelStatsResponse.Stats(4.5, 52.0, 31.25, List.of(0.42)),
                        Instant.parse("2026-08-25T00:00:00Z")))));
        var token = "stored-token".toCharArray();

        var response = beszelRestClient.fetchMetrics(new BeszelMetricsRequest(token));

        var requests = ArgumentCaptor.forClass(ApiRequest.class);
        verify(httpClient, times(2)).execute(requests.capture(), any());
        assertThat(requests.getAllValues()).extracting(ApiRequest::getService, ApiRequest::getApi)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("beszel", "systems"),
                        org.assertj.core.groups.Tuple.tuple("beszel", "latest-stats"));
        assertThat(requests.getAllValues()).allSatisfy(request -> assertThat(request.getHeaders())
                .containsOnlyKeys("Authorization").containsEntry("Authorization", "Bearer stored-token"));
        assertThat(response).containsExactly(new BeszelMetricSystem("Satya WSL", "up",
                Instant.parse("2026-08-25T00:00:00Z"), 4.5, 52.0, 31.25, 0.42));
        assertThat(token).containsOnly('\0');
    }
}
