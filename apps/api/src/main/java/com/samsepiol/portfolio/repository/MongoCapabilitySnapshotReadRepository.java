package com.samsepiol.portfolio.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import lombok.NonNull;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@org.springframework.stereotype.Repository
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
public class MongoCapabilitySnapshotReadRepository implements CapabilitySnapshotReadRepository {
    static final String COLLECTION = "externalSnapshots";
    private static final int MAX_PUBLIC_SNAPSHOTS = CapabilityType.values().length;
    private static final Bson PUBLIC_SNAPSHOT_PROJECTION = Projections.fields(
            Projections.include("capability", "profileId", "state", "title", "sourceLabel", "refreshedAt",
                    "validUntil", "content", "publicApproved", "profileEnabled"),
            Projections.excludeId());

    private final Repository repository;

    public MongoCapabilitySnapshotReadRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public @NonNull Optional<PublicCapabilitySnapshot> findApprovedSnapshot(@NonNull CapabilityType capability) {
        var collection = collection();
        return Optional.ofNullable(collection.find(approvedFilter(capability))
                        .projection(PUBLIC_SNAPSHOT_PROJECTION)
                        .limit(1)
                        .first())
                .map(this::toPublicSnapshot);
    }

    @Override
    public @NonNull List<PublicCapabilitySnapshot> findApprovedSnapshots() {
        var collection = collection();
        return collection.find(approvedFilter())
                .projection(PUBLIC_SNAPSHOT_PROJECTION)
                .limit(MAX_PUBLIC_SNAPSHOTS)
                .into(new java.util.ArrayList<>())
                .stream()
                .map(this::toPublicSnapshot)
                .sorted(Comparator.comparing(snapshot -> snapshot.getCapability().name()))
                .toList();
    }

    private MongoCollection<ExternalSnapshotEntity> collection() {
        return repository.getCollection(COLLECTION, ExternalSnapshotEntity.class);
    }

    private Bson approvedFilter(CapabilityType capability) {
        return and(eq("capability", capability), approvedFilter());
    }

    private Bson approvedFilter() {
        return and(eq("publicApproved", true), eq("profileEnabled", true));
    }

    private PublicCapabilitySnapshot toPublicSnapshot(ExternalSnapshotEntity entity) {
        return PublicCapabilitySnapshot.builder()
                .capability(entity.getCapability())
                .state(entity.getState())
                .title(entity.getTitle())
                .sourceLabel(entity.getSourceLabel())
                .refreshedAt(entity.getRefreshedAt())
                .content(entity.getContent())
                .build();
    }
}
