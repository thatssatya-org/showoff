package com.samsepiol.portfolio.provider;

import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.provider.github.GitHubRepositoryFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubRepositorySnapshotResponse;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Mapper
public interface GitHubRepositorySnapshotMapper {
    GitHubRepositorySnapshotMapper INSTANCE = Mappers.getMapper(GitHubRepositorySnapshotMapper.class);

    @Mapping(target = "capability", expression = "java(com.samsepiol.portfolio.domain.CapabilityType.GITHUB_REPOSITORIES)")
    @Mapping(target = "profileId", source = "properties.profileId")
    @Mapping(target = "state", expression = "java(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY)")
    @Mapping(target = "title", constant = "Featured GitHub repository")
    @Mapping(target = "sourceLabel", constant = "GitHub")
    @Mapping(target = "refreshedAtEpochMillis", source = "refreshedAt", qualifiedByName = "toEpochMillis")
    @Mapping(target = "validUntilEpochMillis", source = "validUntil", qualifiedByName = "toEpochMillis")
    @Mapping(target = "content", source = "response.repository", qualifiedByName = "toContent")
    @Mapping(target = "publicApproved", source = "properties.publicApproved")
    @Mapping(target = "profileEnabled", constant = "true")
    @Mapping(target = "providerEtag", ignore = true)
    ExternalSnapshotEntity toEntity(GitHubRepositoryFetchResponse response, GitHubRefreshProperties properties,
                                    Instant refreshedAt, Instant validUntil);

    @Mapping(target = "refreshedAt", source = "refreshedAtEpochMillis", qualifiedByName = "toInstant")
    PublicCapabilitySnapshot toPublicSnapshot(ExternalSnapshotEntity entity);

    @Named("toEpochMillis")
    default Long toEpochMillis(Instant value) {
        return value.toEpochMilli();
    }

    @Named("toInstant")
    default Instant toInstant(Long value) {
        return Instant.ofEpochMilli(value);
    }

    @Named("toContent")
    default Map<String, String> toContent(GitHubRepositorySnapshotResponse repository) {
        if (repository == null || repository.getNameWithOwner() == null || repository.getNameWithOwner().isBlank()
                || repository.getUrl() == null || repository.getUrl().isBlank() || repository.getStargazerCount() == null
                || repository.getStargazerCount() < 0 || repository.getDefaultBranchRef() == null
                || repository.getDefaultBranchRef().getTarget() == null
                || repository.getDefaultBranchRef().getTarget().getCommittedDate() == null
                || repository.getDefaultBranchRef().getTarget().getCommittedDate().isBlank()) {
            throw new IllegalArgumentException("GitHub repository projection is incomplete");
        }
        Instant.parse(repository.getDefaultBranchRef().getTarget().getCommittedDate());
        var content = new LinkedHashMap<String, String>();
        content.put("repository", repository.getNameWithOwner());
        content.put("url", repository.getUrl());
        content.put("stars", repository.getStargazerCount().toString());
        content.put("latestCommitDate", repository.getDefaultBranchRef().getTarget().getCommittedDate());
        if (repository.getPrimaryLanguage() != null && repository.getPrimaryLanguage().getName() != null
                && !repository.getPrimaryLanguage().getName().isBlank()) {
            content.put("language", repository.getPrimaryLanguage().getName());
        }
        if (repository.getLatestRelease() != null) {
            addIfPresent(content, "latestReleaseTag", repository.getLatestRelease().getTagName());
            addInstantIfPresent(content, "latestReleaseDate", repository.getLatestRelease().getPublishedAt());
            addIfPresent(content, "latestReleaseUrl", repository.getLatestRelease().getUrl());
        }
        return Map.copyOf(content);
    }

    private void addIfPresent(Map<String, String> content, String key, String value) {
        if (value != null && !value.isBlank()) {
            content.put(key, value);
        }
    }

    private void addInstantIfPresent(Map<String, String> content, String key, String value) {
        if (value != null && !value.isBlank()) {
            Instant.parse(value);
            content.put(key, value);
        }
    }
}
