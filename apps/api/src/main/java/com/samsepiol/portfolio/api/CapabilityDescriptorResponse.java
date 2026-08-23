package com.samsepiol.portfolio.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.ComponentType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityDescriptorResponse {
    @NonNull
    CapabilityType capability;
    @NonNull
    ComponentType componentType;
    @NonNull
    String dataEndpoint;
    @NonNull
    String title;
    @NonNull
    String sourceLabel;
    @NonNull
    Instant refreshedAt;
}
