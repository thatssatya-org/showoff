package com.samsepiol.portfolio.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicCapabilitySnapshotTest {

    @Test
    void copiesContentIntoAnImmutableMap() {
        var source = new HashMap<String, Object>();
        source.put("title", "Cached activity");

        var snapshot = PublicCapabilitySnapshot.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .state(CapabilityState.HEALTHY)
                .title("GitHub activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .content(source)
                .build();

        source.put("private", "must not appear");

        assertThatThrownBy(() -> snapshot.getContent().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        org.assertj.core.api.Assertions.assertThat(snapshot.getContent())
                .containsExactlyEntriesOf(java.util.Map.of("title", "Cached activity"));
    }
}
