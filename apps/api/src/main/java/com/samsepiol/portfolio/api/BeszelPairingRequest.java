package com.samsepiol.portfolio.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
public class BeszelPairingRequest {
    @NonNull
    @ToString.Exclude
    String token;

    public static BeszelPairingRequest from(JsonNode requestBody) {
        if (requestBody == null || !requestBody.isObject() || requestBody.size() != 1
                || !requestBody.has("token") || !requestBody.path("token").isTextual()) {
            throw new BeszelPairingRequestException();
        }
        var token = requestBody.path("token").textValue();
        if (token == null || token.isBlank() || token.length() > 1_024) {
            throw new BeszelPairingRequestException();
        }
        return BeszelPairingRequest.builder().token(token).build();
    }
}
