package com.samsepiol.portfolio.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListmonkSubscriberRequest {
    @NonNull
    String email;
    @NonNull
    String status;
    @NonNull
    List<Long> lists;
    boolean preconfirm;
}
