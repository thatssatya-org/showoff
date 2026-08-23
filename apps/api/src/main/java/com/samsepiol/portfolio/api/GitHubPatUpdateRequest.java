package com.samsepiol.portfolio.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public class GitHubPatUpdateRequest {
    @NonNull
    @ToString.Exclude
    String token;

    public static GitHubPatUpdateRequest from(JsonNode requestBody) {
        if (requestBody == null || !requestBody.isObject() || requestBody.size() != 1
                || !requestBody.has("token") || !requestBody.path("token").isTextual()) {
            throw new GitHubPatRequestException();
        }
        var token = requestBody.path("token").textValue();
        if (token == null || token.isBlank() || token.length() > 1024) {
            throw new GitHubPatRequestException();
        }
        return GitHubPatUpdateRequest.builder().token(token).build();
    }
}
