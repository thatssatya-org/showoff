package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.domain.CapabilityType;
import lombok.NonNull;

public interface CapabilitySnapshotStrategy {
    @NonNull
    CapabilityType capabilityType();

    @NonNull
    CapabilitySnapshotRefreshResponse refresh(@NonNull CapabilitySnapshotRefreshRequest request);
}
