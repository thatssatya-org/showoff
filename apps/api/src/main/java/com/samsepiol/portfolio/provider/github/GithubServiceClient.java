package com.samsepiol.portfolio.provider.github;

import lombok.experimental.UtilityClass;

public interface GithubServiceClient {
    GitHubActivityFetchResponse fetchPublicEvents(char[] token, String etag);

    GitHubContributionFetchResponse fetchContributionCalendar(char[] token, GitHubContributionCalendarRequest request);

    @UtilityClass
    class Constants {
        public static final String SERVICE = "github";
        public static final String PUBLIC_EVENTS = "public-events";
        public static final String CONTRIBUTION_CALENDAR = "contribution-calendar";
        public static final String ACCEPT = "application/vnd.github+json";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String APPLICATION_JSON = "application/json";
        public static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
        public static final String API_VERSION = "2022-11-28";
        public static final String USER_AGENT = "User-Agent";
        public static final String USER_AGENT_VALUE = "showoff-github-refresh";
        public static final String IF_NONE_MATCH = "If-None-Match";
        public static final String ETAG = "etag";
    }
}
