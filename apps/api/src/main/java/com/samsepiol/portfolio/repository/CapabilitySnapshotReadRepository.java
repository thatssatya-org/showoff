package com.samsepiol.portfolio.repository;

import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public interface CapabilitySnapshotReadRepository {
    @NonNull
    Optional<PublicCapabilitySnapshot> findApprovedSnapshot(@NonNull CapabilityType capability);

    @NonNull
    List<PublicCapabilitySnapshot> findApprovedSnapshots();
}
