package com.samsepiol.portfolio.provider.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryRequest {
    public static final String QUERY = """
            query RepositorySnapshot($owner: String!, $name: String!) {
              repository(owner: $owner, name: $name) {
                name
                nameWithOwner
                isPrivate
                visibility
                url
                stargazerCount
                primaryLanguage {
                  name
                }
                defaultBranchRef {
                  target {
                    ... on Commit {
                      committedDate
                    }
                  }
                }
                latestRelease {
                  tagName
                  publishedAt
                  url
                }
              }
            }
            """;

    @NonNull
    String query;
    @NonNull
    Map<String, String> variables;
    @NonNull
    @JsonIgnore
    String expectedOwner;
    @NonNull
    @JsonIgnore
    String expectedName;
}
