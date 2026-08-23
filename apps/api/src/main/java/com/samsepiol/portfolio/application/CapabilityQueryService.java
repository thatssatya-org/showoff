package com.samsepiol.portfolio.application;

import com.samsepiol.portfolio.api.CapabilityDescriptorResponse;
import com.samsepiol.portfolio.api.CapabilitySnapshotResponse;
import com.samsepiol.portfolio.api.CapabilitySnapshotResult;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.PublicCapabilitySnapshot;
import com.samsepiol.portfolio.repository.CapabilitySnapshotReadRepository;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.Optional;

@Service
public class CapabilityQueryService {
    private static final String CAPABILITY_PATH_PREFIX = "/api/v1/capabilities/";

    private final CapabilitySnapshotReadRepository capabilitySnapshotReadRepository;

    public CapabilityQueryService(CapabilitySnapshotReadRepository capabilitySnapshotReadRepository) {
        this.capabilitySnapshotReadRepository = capabilitySnapshotReadRepository;
    }

    @NonNull
    public java.util.List<CapabilityDescriptorResponse> findAvailableDescriptors() {
        return capabilitySnapshotReadRepository.findApprovedSnapshots().stream()
                .map(this::toDescriptor)
                .sorted(Comparator.comparing(descriptor -> descriptor.getCapability().name()))
                .toList();
    }

    @NonNull
    public Optional<CapabilitySnapshotResult> findSnapshot(@NonNull CapabilityType capability) {
        return capabilitySnapshotReadRepository.findApprovedSnapshot(capability)
                .map(this::toResult);
    }

    private CapabilityDescriptorResponse toDescriptor(PublicCapabilitySnapshot snapshot) {
        return CapabilityDescriptorResponse.builder()
                .capability(snapshot.getCapability())
                .componentType(snapshot.getCapability().componentType())
                .dataEndpoint(CAPABILITY_PATH_PREFIX + snapshot.getCapability().apiValue())
                .title(snapshot.getTitle())
                .sourceLabel(snapshot.getSourceLabel())
                .refreshedAt(snapshot.getRefreshedAt())
                .build();
    }

    private CapabilitySnapshotResult toResult(PublicCapabilitySnapshot snapshot) {
        var response = CapabilitySnapshotResponse.builder()
                .capability(snapshot.getCapability())
                .componentType(snapshot.getCapability().componentType())
                .state(snapshot.getState())
                .title(snapshot.getTitle())
                .sourceLabel(snapshot.getSourceLabel())
                .refreshedAt(snapshot.getRefreshedAt())
                .content(snapshot.getContent())
                .build();

        return CapabilitySnapshotResult.builder()
                .response(response)
                .etag(calculateEtag(response))
                .build();
    }

    private String calculateEtag(CapabilitySnapshotResponse response) {
        var canonicalContent = response.getContent().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "&" + right);
        var canonicalResponse = String.join("|",
                response.getCapability().name(),
                response.getComponentType().name(),
                response.getState().name(),
                response.getTitle(),
                response.getSourceLabel(),
                response.getRefreshedAt().toString(),
                canonicalContent);

        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalResponse.getBytes(StandardCharsets.UTF_8));
            return "\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
