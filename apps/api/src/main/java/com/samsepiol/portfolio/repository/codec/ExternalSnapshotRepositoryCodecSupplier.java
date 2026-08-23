package com.samsepiol.portfolio.repository.codec;

import com.samsepiol.library.mongo.codec.CodecSupplier;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalSnapshotRepositoryCodecSupplier implements CodecSupplier {
    @Override
    public @NonNull List<Class<?>> getManagedClasses() {
        return List.of(ExternalSnapshotEntity.class);
    }
}
