package com.samsepiol.portfolio.application;

import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.repository.CapabilitySnapshotReadRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityQueryServiceTest {

    @Test
    void mapsAnApprovedSnapshotToAPublicResponseWithAStableEtag() {
        var snapshot = fixture();
        var service = new CapabilityQueryService(new FixedCapabilitySnapshotReadRepository(snapshot));

        var firstResponse = service.findSnapshot(CapabilityType.GITHUB_ACTIVITY);
        var secondResponse = service.findSnapshot(CapabilityType.GITHUB_ACTIVITY);

        assertThat(firstResponse).isPresent();
        assertThat(secondResponse).isPresent();
        assertThat(firstResponse.orElseThrow().getEtag()).isEqualTo(secondResponse.orElseThrow().getEtag());
        assertThat(firstResponse.orElseThrow().getResponse().getState()).isEqualTo(CapabilityState.HEALTHY);
        assertThat(firstResponse.orElseThrow().getResponse().getContent())
                .containsExactlyEntriesOf(Map.of("events", "5"));
    }

    @Test
    void omitsUnavailableCapabilitiesFromTheDescriptorList() {
        var service = new CapabilityQueryService(new FixedCapabilitySnapshotReadRepository(fixture()));

        var descriptors = service.findAvailableDescriptors();

        assertThat(descriptors).singleElement().satisfies(descriptor -> {
            assertThat(descriptor.getCapability()).isEqualTo(CapabilityType.GITHUB_ACTIVITY);
            assertThat(descriptor.getDataEndpoint()).isEqualTo("/api/v1/capabilities/github-activity");
        });
    }

    private static PublicCapabilitySnapshot fixture() {
        return PublicCapabilitySnapshot.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .state(CapabilityState.HEALTHY)
                .title("Public activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .content(Map.of("events", "5"))
                .build();
    }

    private record FixedCapabilitySnapshotReadRepository(PublicCapabilitySnapshot snapshot)
            implements CapabilitySnapshotReadRepository {
        @Override
        public Optional<PublicCapabilitySnapshot> findApprovedSnapshot(CapabilityType capability) {
            return snapshot.getCapability() == capability ? Optional.of(snapshot) : Optional.empty();
        }

        @Override
        public List<PublicCapabilitySnapshot> findApprovedSnapshots() {
            return List.of(snapshot);
        }
    }
}
