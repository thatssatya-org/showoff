package com.samsepiol.portfolio.provider.beszel;

import java.util.List;

/**
 * Backend-only contract for Beszel's REST surface. Implementations must use
 * the encrypted pairing token internally and return a deliberately minimized
 * model; neither Beszel URLs nor raw records may cross the public API boundary.
 */
public interface BeszelRestClient {
    List<BeszelMetricSystem> fetchMetrics(BeszelMetricsRequest request);
}
