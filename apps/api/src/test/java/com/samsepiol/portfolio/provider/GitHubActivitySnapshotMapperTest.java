package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.provider.github.GitHubActivityFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubPublicEventResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubActivitySnapshotMapperTest {
    private final GitHubActivitySnapshotMapper mapper = GitHubActivitySnapshotMapper.INSTANCE;

    @Test
    void persistsOnlyTheApprovedPublicEventProjectionInTheDefaultZone() {
        var response = GitHubActivityFetchResponse.builder().statusCode(200).etag("\"next\"")
                .events(List.of(GitHubPublicEventResponse.builder().type("PushEvent")
                        .createdAt("2026-08-23T20:00:00Z")
                        .repo(GitHubRepositoryResponse.builder().name("owner/public-repo").build())
                        .build()))
                .build();
        var properties = refreshProperties();

        var entity = mapper.toEntity(response, properties, Instant.EPOCH, Instant.EPOCH.plusSeconds(900));

        assertThat(entity.getProviderEtag()).isEqualTo("\"next\"");
        assertThat(entity.getContent().get("events"))
                .contains("PushEvent", "2026-08-24", "owner/public-repo");
    }

    private static GitHubRefreshProperties refreshProperties() {
        var properties = new GitHubRefreshProperties();
        properties.setEnabled(true);
        properties.setPublicApproved(true);
        properties.setProfileId("github-primary");
        properties.setHandle("octocat");
        properties.setCron("0 */15 * * * *");
        return properties;
    }
}
