package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.domain.CapabilityType;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public final class CapabilitySnapshotStrategyFactory {
    private final Map<CapabilityType, CapabilitySnapshotStrategy> strategies;

    public CapabilitySnapshotStrategyFactory(List<CapabilitySnapshotStrategy> injectedStrategies) {
        var resolvedStrategies = new EnumMap<CapabilityType, CapabilitySnapshotStrategy>(CapabilityType.class);

        for (var strategy : List.copyOf(injectedStrategies)) {
            var priorStrategy = resolvedStrategies.putIfAbsent(strategy.capabilityType(), strategy);
            if (priorStrategy != null) {
                throw new IllegalStateException("Duplicate capability snapshot strategy: " + strategy.capabilityType());
            }
        }

        strategies = Collections.unmodifiableMap(new EnumMap<>(resolvedStrategies));
    }

    @NonNull
    public Optional<CapabilitySnapshotStrategy> find(@NonNull CapabilityType capability) {
        return Optional.ofNullable(strategies.get(capability));
    }
}
