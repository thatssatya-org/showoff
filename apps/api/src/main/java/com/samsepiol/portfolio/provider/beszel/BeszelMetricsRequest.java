package com.samsepiol.portfolio.provider.beszel;

import lombok.NonNull;

/** Typed boundary input; provider credentials never leave this package. */
public record BeszelMetricsRequest(@NonNull char[] token) {
}
