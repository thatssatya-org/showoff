package com.samsepiol.portfolio.provider.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryGraphQlResponse {
    GitHubRepositoryGraphQlDataResponse data;
    List<GitHubRepositoryGraphQlErrorResponse> errors;

    public boolean hasErrors() {
        return CollectionUtils.isNotEmpty(errors);
    }

    public boolean repositoryMatches(GitHubRepositoryRequest request) {
        var repository = data == null ? null : data.getRepository();
        return repository != null && repository.matches(request);
    }
}
