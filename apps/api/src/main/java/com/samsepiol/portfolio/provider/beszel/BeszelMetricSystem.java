package com.samsepiol.portfolio.provider.beszel;

import java.time.Instant;

/** Private, deliberately small view used by the Tailnet metrics surface. */
public record BeszelMetricSystem(
        String name,
        String state,
        Instant observedAt,
        Double cpuPercent,
        Double memoryPercent,
        Double diskPercent,
        Double loadAverage) {
}
