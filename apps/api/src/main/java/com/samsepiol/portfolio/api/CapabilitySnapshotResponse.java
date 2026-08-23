package com.samsepiol.portfolio.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.ComponentType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

@Value
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilitySnapshotResponse {
    @NonNull
    CapabilityType capability;
    @NonNull
    ComponentType componentType;
    @NonNull
    CapabilityState state;
    @NonNull
    String title;
    @NonNull
    String sourceLabel;
    @NonNull
    Instant refreshedAt;
    @NonNull
    Map<String, Object> content;

    @Builder
    public CapabilitySnapshotResponse(
            @NonNull CapabilityType capability,
            @NonNull ComponentType componentType,
            @NonNull CapabilityState state,
            @NonNull String title,
            @NonNull String sourceLabel,
            @NonNull Instant refreshedAt,
            @NonNull Map<String, Object> content) {
        this.capability = capability;
        this.componentType = componentType;
        this.state = state;
        this.title = title;
        this.sourceLabel = sourceLabel;
        this.refreshedAt = refreshedAt;
        this.content = Map.copyOf(content);
    }
}
