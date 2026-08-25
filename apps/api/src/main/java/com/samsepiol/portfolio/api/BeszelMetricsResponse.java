package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.provider.beszel.BeszelMetricSystem;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/** The Tailnet UI contract. It deliberately omits provider topology and IDs. */
@Value
@Builder
@Jacksonized
public class BeszelMetricsResponse {
    @NonNull
    Instant refreshedAt;
    boolean stale;
    @NonNull
    List<BeszelMetricSystem> systems;
}
