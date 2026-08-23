package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.provider.github.GitHubActivityFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubPublicEventResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubActivitySnapshotMapperTest {
    private final GitHubActivitySnapshotMapper mapper = Mappers.getMapper(GitHubActivitySnapshotMapper.class);

    @Test
    void persistsOnlyTheApprovedPublicEventProjectionInTheDefaultZone() {
        var response = GitHubActivityFetchResponse.builder().statusCode(200).etag("\"next\"")
                .events(List.of(GitHubPublicEventResponse.builder().type("PushEvent")
                        .createdAt("2026-08-23T20:00:00Z")
                        .repo(GitHubRepositoryResponse.builder().name("owner/public-repo").build())
                        .build()))
                .build();
        var properties = new GitHubRefreshProperties(true, true, "github-primary", "octocat", "0 */15 * * * *");

        var entity = mapper.toEntity(response, properties, Instant.EPOCH, Instant.EPOCH.plusSeconds(900));

        assertThat(entity.getProviderEtag()).isEqualTo("\"next\"");
        assertThat(entity.getContent().get("events"))
                .contains("PushEvent", "2026-08-24", "owner/public-repo");
    }
}
