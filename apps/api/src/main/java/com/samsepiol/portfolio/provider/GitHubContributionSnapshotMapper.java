package com.samsepiol.portfolio.provider;

import com.samsepiol.library.core.util.SerializationUtil;
import com.samsepiol.portfolio.configuration.GitHubRefreshProperties;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.provider.github.GitHubContributionCalendarResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionDayResponse;
import com.samsepiol.portfolio.provider.github.GitHubContributionFetchResponse;
import com.samsepiol.portfolio.repository.entity.ExternalSnapshotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Mapper
public interface GitHubContributionSnapshotMapper {
    int MAX_CONTRIBUTION_DAYS = 371;
    GitHubContributionSnapshotMapper INSTANCE = Mappers.getMapper(GitHubContributionSnapshotMapper.class);

    @Mapping(target = "count", source = "contributionCount")
    GitHubContributionDay toPublicDay(GitHubContributionDayResponse response);

    @Mapping(target = "capability", expression = "java(com.samsepiol.portfolio.domain.CapabilityType.GITHUB_CONTRIBUTIONS)")
    @Mapping(target = "profileId", source = "properties.profileId")
    @Mapping(target = "state", expression = "java(com.samsepiol.portfolio.domain.CapabilityState.HEALTHY)")
    @Mapping(target = "title", constant = "GitHub contributions")
    @Mapping(target = "sourceLabel", constant = "GitHub")
    @Mapping(target = "refreshedAtEpochMillis", source = "refreshedAt", qualifiedByName = "toEpochMillis")
    @Mapping(target = "validUntilEpochMillis", source = "validUntil", qualifiedByName = "toEpochMillis")
    @Mapping(target = "content", source = "response.contributionCalendar", qualifiedByName = "toContent")
    @Mapping(target = "publicApproved", source = "properties.publicApproved")
    @Mapping(target = "profileEnabled", constant = "true")
    @Mapping(target = "providerEtag", ignore = true)
    ExternalSnapshotEntity toEntity(GitHubContributionFetchResponse response, GitHubRefreshProperties properties,
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
    default Map<String, String> toContent(GitHubContributionCalendarResponse calendar) {
        if (calendar == null || calendar.getTotalContributions() == null || calendar.getTotalContributions() < 0) {
            throw new IllegalArgumentException("GitHub contribution calendar is incomplete");
        }
        var contributionDays = contributionDays(calendar);
        return Map.of(
                "totalContributions", calendar.getTotalContributions().toString(),
                "includesPrivateContributions", Boolean.TRUE.toString(),
                "contributionDays", SerializationUtil.convertToString(contributionDays));
    }

    private List<GitHubContributionDay> contributionDays(GitHubContributionCalendarResponse calendar) {
        if (calendar.getWeeks() == null) {
            throw new IllegalArgumentException("GitHub contribution calendar weeks are missing");
        }
        var contributionDays = calendar.getWeeks().stream()
                .flatMap(week -> week == null || week.getContributionDays() == null
                        ? Stream.empty()
                        : week.getContributionDays().stream())
                .limit(MAX_CONTRIBUTION_DAYS)
                .peek(this::validateContributionDay)
                .map(this::toPublicDay)
                .toList();
        if (contributionDays.isEmpty()) {
            throw new IllegalArgumentException("GitHub contribution calendar contains no days");
        }
        return contributionDays;
    }

    private void validateContributionDay(GitHubContributionDayResponse contributionDay) {
        if (contributionDay == null || contributionDay.getDate() == null || contributionDay.getContributionCount() == null
                || contributionDay.getContributionCount() < 0) {
            throw new IllegalArgumentException("GitHub contribution day is incomplete");
        }
        LocalDate.parse(contributionDay.getDate());
    }
}
