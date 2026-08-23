package com.samsepiol.portfolio.repository;

import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public final class EmptyCapabilitySnapshotReadRepository implements CapabilitySnapshotReadRepository {
    @Override
    @NonNull
    public Optional<PublicCapabilitySnapshot> findApprovedSnapshot(@NonNull CapabilityType capability) {
        return Optional.empty();
    }

    @Override
    @NonNull
    public List<PublicCapabilitySnapshot> findApprovedSnapshots() {
        return List.of();
    }
}
