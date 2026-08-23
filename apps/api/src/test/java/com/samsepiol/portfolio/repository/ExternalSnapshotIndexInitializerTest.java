package com.samsepiol.portfolio.repository;

import com.mongodb.client.MongoCollection;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalSnapshotIndexInitializerTest {
    @Test
    void createsRequiredIndexesAtApplicationStartup() {
        Repository repository = mock(Repository.class);
        @SuppressWarnings("unchecked")
        MongoCollection<ExternalSnapshotEntity> collection = mock(MongoCollection.class);
        when(repository.getCollection(MongoCapabilitySnapshotReadRepository.COLLECTION, ExternalSnapshotEntity.class))
                .thenReturn(collection);

        new ExternalSnapshotIndexInitializer(repository).run(new DefaultApplicationArguments());

        verify(collection).createIndex(any(Bson.class), any());
        verify(collection, times(2)).createIndex(any(Bson.class));
    }
}
