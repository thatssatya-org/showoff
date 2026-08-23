package com.samsepiol.portfolio.repository;

import com.mongodb.client.model.IndexOptions;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.mongodb.client.model.Indexes.ascending;

@Component
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
public class ExternalSnapshotIndexInitializer implements ApplicationRunner {
    private final Repository repository;

    public ExternalSnapshotIndexInitializer(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        var collection = repository.getCollection(MongoCapabilitySnapshotReadRepository.COLLECTION,
                ExternalSnapshotEntity.class);
        collection.createIndex(ascending("capability", "profileId"), new IndexOptions().unique(true));
        collection.createIndex(ascending("capability", "publicApproved", "profileEnabled"));
        collection.createIndex(ascending("publicApproved", "profileEnabled", "capability"));
    }
}
