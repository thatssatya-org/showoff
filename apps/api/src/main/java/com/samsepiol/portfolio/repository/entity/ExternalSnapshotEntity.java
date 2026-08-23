package com.samsepiol.portfolio.repository.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.samsepiol.library.repository.models.Entity;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.Map;

@Value
@Jacksonized
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ExternalSnapshotEntity extends Entity {
    private static final String ID_PREFIX = "ES";

    @NonNull
    @BsonProperty("capability")
    CapabilityType capability;
    @NonNull
    @BsonProperty("profileId")
    String profileId;
    @NonNull
    @BsonProperty("state")
    CapabilityState state;
    @NonNull
    @BsonProperty("title")
    String title;
    @NonNull
    @BsonProperty("sourceLabel")
    String sourceLabel;
    @NonNull
    @BsonProperty("refreshedAt")
    Instant refreshedAt;
    @NonNull
    @BsonProperty("validUntil")
    Instant validUntil;
    @NonNull
    @BsonProperty("content")
    Map<String, String> content;
    @BsonProperty("publicApproved")
    boolean publicApproved;
    @BsonProperty("profileEnabled")
    boolean profileEnabled;
    @BsonProperty("providerEtag")
    String providerEtag;

    @BsonCreator
    public ExternalSnapshotEntity(
            @NonNull @BsonProperty("capability") CapabilityType capability,
            @NonNull @BsonProperty("profileId") String profileId,
            @NonNull @BsonProperty("state") CapabilityState state,
            @NonNull @BsonProperty("title") String title,
            @NonNull @BsonProperty("sourceLabel") String sourceLabel,
            @NonNull @BsonProperty("refreshedAt") Instant refreshedAt,
            @NonNull @BsonProperty("validUntil") Instant validUntil,
            @NonNull @BsonProperty("content") Map<String, String> content,
            @BsonProperty("publicApproved") boolean publicApproved,
            @BsonProperty("profileEnabled") boolean profileEnabled,
            @BsonProperty("providerEtag") String providerEtag) {
        this.capability = capability;
        this.profileId = profileId;
        this.state = state;
        this.title = title;
        this.sourceLabel = sourceLabel;
        this.refreshedAt = refreshedAt;
        this.validUntil = validUntil;
        this.content = Map.copyOf(content);
        this.publicApproved = publicApproved;
        this.profileEnabled = profileEnabled;
        this.providerEtag = providerEtag;
    }

    protected ExternalSnapshotEntity(ExternalSnapshotEntityBuilder<?, ?> builder) {
        this(builder.capability, builder.profileId, builder.state, builder.title, builder.sourceLabel,
                builder.refreshedAt, builder.validUntil, builder.content, builder.publicApproved,
                builder.profileEnabled, builder.providerEtag);
    }

    @Override
    protected @NonNull String getIdPrefix() {
        return ID_PREFIX;
    }
}
