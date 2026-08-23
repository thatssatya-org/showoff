package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.domain.CapabilityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapabilitySnapshotStrategyFactoryTest {

    @Test
    void failsFastWhenTwoStrategiesClaimTheSameCapability() {
        var first = mock(CapabilitySnapshotStrategy.class);
        var second = mock(CapabilitySnapshotStrategy.class);
        when(first.capabilityType()).thenReturn(CapabilityType.GITHUB_ACTIVITY);
        when(second.capabilityType()).thenReturn(CapabilityType.GITHUB_ACTIVITY);

        assertThatThrownBy(() -> new CapabilitySnapshotStrategyFactory(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Duplicate capability snapshot strategy: GITHUB_ACTIVITY");
    }

    @Test
    void permitsMissingOptionalStrategies() {
        var factory = new CapabilitySnapshotStrategyFactory(List.of());

        assertThat(factory.find(CapabilityType.SPOTIFY_ON_REPEAT)).isEmpty();
    }
}
