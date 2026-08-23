package com.samsepiol.portfolio.configuration;

import com.samsepiol.portfolio.repository.CapabilitySnapshotReadRepository;
import com.samsepiol.portfolio.repository.EmptyCapabilitySnapshotReadRepository;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.library.mongo.impl.DefaultRepository;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class CapabilityQueryConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
    @ConditionalOnMissingBean(Repository.class)
    Repository mongoRepository(MongoTemplate mongoTemplate, CodecRegistry codecRegistry) {
        return new DefaultRepository(mongoTemplate, codecRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(CapabilitySnapshotReadRepository.class)
    CapabilitySnapshotReadRepository emptyCapabilitySnapshotReadRepository() {
        return new EmptyCapabilitySnapshotReadRepository();
    }
}
