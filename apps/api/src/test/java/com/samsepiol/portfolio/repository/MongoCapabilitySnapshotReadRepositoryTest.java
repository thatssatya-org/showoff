package com.samsepiol.portfolio.repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoCapabilitySnapshotReadRepositoryTest {

    @Test
    void performsAProjectedBoundedCapabilityRead() {
        Repository repository = mock(Repository.class);
        @SuppressWarnings("unchecked")
        MongoCollection<ExternalSnapshotEntity> collection = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        FindIterable<ExternalSnapshotEntity> iterable = mock(FindIterable.class);
        var snapshot = fixture(CapabilityType.GITHUB_ACTIVITY);
        when(repository.getCollection(MongoCapabilitySnapshotReadRepository.COLLECTION, ExternalSnapshotEntity.class))
                .thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.projection(any(Bson.class))).thenReturn(iterable);
        when(iterable.limit(1)).thenReturn(iterable);
        when(iterable.first()).thenReturn(snapshot);

        var result = new MongoCapabilitySnapshotReadRepository(repository)
                .findApprovedSnapshot(CapabilityType.GITHUB_ACTIVITY);

        assertThat(result).hasValueSatisfying(publicSnapshot -> {
            assertThat(publicSnapshot.getCapability()).isEqualTo(CapabilityType.GITHUB_ACTIVITY);
            assertThat(publicSnapshot.getContent()).containsEntry("events", "5");
        });
        org.mockito.Mockito.verify(iterable).projection(any(Bson.class));
        org.mockito.Mockito.verify(iterable).limit(1);
    }

    @Test
    void boundsTheCapabilityManifestReadToTheKnownCapabilitySet() {
        Repository repository = mock(Repository.class);
        @SuppressWarnings("unchecked")
        MongoCollection<ExternalSnapshotEntity> collection = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        FindIterable<ExternalSnapshotEntity> iterable = mock(FindIterable.class);
        when(repository.getCollection(MongoCapabilitySnapshotReadRepository.COLLECTION, ExternalSnapshotEntity.class))
                .thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.projection(any(Bson.class))).thenReturn(iterable);
        when(iterable.limit(CapabilityType.values().length)).thenReturn(iterable);
        when(iterable.into(any(Collection.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var target = (Collection<ExternalSnapshotEntity>) invocation.getArgument(0);
            target.add(fixture(CapabilityType.GITHUB_ACTIVITY));
            return target;
        });

        var snapshots = new MongoCapabilitySnapshotReadRepository(repository).findApprovedSnapshots();

        assertThat(snapshots).hasSize(1);
        org.mockito.Mockito.verify(iterable).limit(CapabilityType.values().length);
    }

    @Test
    void defensivelyCopiesSnapshotContent() {
        var content = new HashMap<String, Object>();
        content.put("events", "5");
        var snapshot = ExternalSnapshotEntity.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .profileId("profile-1")
                .state(CapabilityState.HEALTHY)
                .title("Public activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .validUntil(Instant.parse("2026-08-23T13:00:00Z"))
                .content(content)
                .publicApproved(true)
                .profileEnabled(true)
                .build();

        content.put("events", "6");

        assertThat(snapshot.getContent()).containsEntry("events", "5");
        assertThatThrownBy(() -> snapshot.getContent().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static ExternalSnapshotEntity fixture(CapabilityType capability) {
        return ExternalSnapshotEntity.builder()
                .capability(capability)
                .profileId("profile-1")
                .state(CapabilityState.HEALTHY)
                .title("Public activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .validUntil(Instant.parse("2026-08-23T13:00:00Z"))
                .content(java.util.Map.of("events", "5"))
                .publicApproved(true)
                .profileEnabled(true)
                .build();
    }
}
