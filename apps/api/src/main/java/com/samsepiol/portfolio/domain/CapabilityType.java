package com.samsepiol.portfolio.domain;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CapabilityType {
    GITHUB_ACTIVITY(ProviderType.GITHUB, ComponentType.ACTIVITY_TIMELINE),
    GITHUB_CONTRIBUTIONS(ProviderType.GITHUB, ComponentType.CONTRIBUTION_HEATMAP),
    GITHUB_REPOSITORIES(ProviderType.GITHUB, ComponentType.REPOSITORY_GRID),
    SPOTIFY_ON_REPEAT(ProviderType.SPOTIFY, ComponentType.MUSIC_CARD),
    INSTAGRAM_MEDIA(ProviderType.INSTAGRAM, ComponentType.SOCIAL_GRID),
    YOUTUBE_UPLOADS(ProviderType.YOUTUBE, ComponentType.SOCIAL_GRID),
    LINKEDIN_SELECTED_POSTS(ProviderType.LINKEDIN, ComponentType.SOCIAL_GRID),
    HOMELAB_SUMMARY(ProviderType.HOMELAB, ComponentType.HOMELAB_SUMMARY);

    private final ProviderType providerType;
    private final ComponentType componentType;

    CapabilityType(ProviderType providerType, ComponentType componentType) {
        this.providerType = providerType;
        this.componentType = componentType;
    }

    @NonNull
    public ProviderType providerType() {
        return providerType;
    }

    @NonNull
    public ComponentType componentType() {
        return componentType;
    }

    @NonNull
    public String apiValue() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @NonNull
    public static Optional<CapabilityType> fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        var normalizedValue = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
                .filter(capabilityType -> capabilityType.name().equals(normalizedValue))
                .findFirst();
    }
}
