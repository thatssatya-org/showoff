package com.samsepiol.portfolio.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class CapabilitySnapshotResult {
    @NonNull
    CapabilitySnapshotResponse response;
    @NonNull
    String etag;
}
