package com.samsepiol.portfolio.provider;

import com.samsepiol.library.core.util.DateTimeUtils;
import com.samsepiol.library.core.util.SerializationUtil;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.provider.github.GitHubActivityFetchResponse;
import com.samsepiol.portfolio.provider.github.GitHubPublicEventResponse;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface GitHubActivitySnapshotMapper {
    @Mapping(target = "day", source = "createdAt", qualifiedByName = "toDefaultZoneDate")
    @Mapping(target = "repository", source = "repo.name")
    GitHubPublicEvent toPublicEvent(GitHubPublicEventResponse event);

    @Mapping(target = "capability", expression = "java(com.samsepiol.portfolio.domain.CapabilityType.GITHUB_ACTIVITY)")
    @Mapping(target = "profileId", source = "properties.profileId")
    @Mapping(target = "state", expression = "java(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY)")
    @Mapping(target = "title", constant = "Recent public activity")
    @Mapping(target = "sourceLabel", constant = "GitHub")
    @Mapping(target = "refreshedAt", source = "refreshedAt")
    @Mapping(target = "validUntil", source = "validUntil")
    @Mapping(target = "content", source = "response.events", qualifiedByName = "toContent")
    @Mapping(target = "publicApproved", source = "properties.publicApproved")
    @Mapping(target = "profileEnabled", constant = "true")
    @Mapping(target = "providerEtag", source = "response.etag")
    ExternalSnapshotEntity toEntity(GitHubActivityFetchResponse response, GitHubRefreshProperties properties,
                                    Instant refreshedAt, Instant validUntil);

    PublicCapabilitySnapshot toPublicSnapshot(ExternalSnapshotEntity entity);

    @Named("toDefaultZoneDate")
    default String toDefaultZoneDate(String value) {
        return DateTimeUtils.toDefaultZoneDate(Instant.parse(value)).toString();
    }

    @Named("toContent")
    default Map<String, String> toContent(List<GitHubPublicEventResponse> events) {
        var publicEvents = events.stream()
                .filter(event -> event.getType() != null && !event.getType().isBlank())
                .filter(event -> event.getCreatedAt() != null && !event.getCreatedAt().isBlank())
                .filter(event -> event.getRepo() != null && event.getRepo().getName() != null
                        && !event.getRepo().getName().isBlank())
                .limit(8)
                .map(this::toPublicEvent)
                .toList();
        return Map.of("events", SerializationUtil.convertToString(publicEvents));
    }
}
