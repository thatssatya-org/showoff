package com.samsepiol.portfolio.repository;

import com.mongodb.client.model.Projections;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@org.springframework.stereotype.Repository
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
@RequiredArgsConstructor
public class GitHubActivitySnapshotRepository {
    private static final Bson REFRESH_PROJECTION = Projections.fields(Projections.include("capability", "profileId",
            "state", "title", "sourceLabel", "refreshedAt", "validUntil", "content", "publicApproved",
            "profileEnabled", "providerEtag"), Projections.excludeId());
    private final Repository repository;

    public ExternalSnapshotEntity find(@NonNull String profileId) {
        return repository.getCollection(MongoCapabilitySnapshotReadRepository.COLLECTION,
                        ExternalSnapshotEntity.class)
                .find(and(eq("capability", CapabilityType.GITHUB_ACTIVITY), eq("profileId", profileId)))
                .projection(REFRESH_PROJECTION).limit(1).first();
    }

    public void replace(@NonNull ExternalSnapshotEntity snapshot) {
        repository.upsert(MongoCapabilitySnapshotReadRepository.COLLECTION, snapshot,
                and(eq("capability", snapshot.getCapability()), eq("profileId", snapshot.getProfileId())));
    }
}
