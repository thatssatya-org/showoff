package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.application.CapabilityQueryService;
import com.samsepiol.portfolio.domain.CapabilityState;
import com.samsepiol.portfolio.domain.CapabilityType;
import com.samsepiol.portfolio.domain.ComponentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CapabilityController.class)
@Import(ApiExceptionHandler.class)
class CapabilityControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CapabilityQueryService capabilityQueryService;

    @Test
    void returnsAnExactBodylessNoContentResponseForAnAbsentCapability() throws Exception {
        when(capabilityQueryService.findSnapshot(CapabilityType.SPOTIFY_ON_REPEAT)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/capabilities/spotify-on-repeat"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void returnsAProjectedSnapshotAndEtagForAnApprovedCapability() throws Exception {
        var response = CapabilitySnapshotResponse.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .componentType(ComponentType.ACTIVITY_TIMELINE)
                .state(CapabilityState.HEALTHY)
                .title("Public activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .content(Map.of("events", "5"))
                .build();
        when(capabilityQueryService.findSnapshot(CapabilityType.GITHUB_ACTIVITY))
                .thenReturn(Optional.of(CapabilitySnapshotResult.builder()
                        .response(response)
                        .etag("\"capability-etag\"")
                        .build()));

        mockMvc.perform(get("/api/v1/capabilities/github-activity"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"capability-etag\""))
                .andExpect(header().string("Cache-Control", "max-age=300, public, stale-while-revalidate=86400"))
                .andExpect(jsonPath("$.capability").value("GITHUB_ACTIVITY"))
                .andExpect(jsonPath("$.state").value("HEALTHY"))
                .andExpect(jsonPath("$.content.events").value("5"));
    }

    @Test
    void returnsNotModifiedWhenTheVisitorHasTheCurrentEtag() throws Exception {
        var response = CapabilitySnapshotResponse.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .componentType(ComponentType.ACTIVITY_TIMELINE)
                .state(CapabilityState.STALE)
                .title("Cached activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .content(Map.of("events", "5"))
                .build();
        when(capabilityQueryService.findSnapshot(CapabilityType.GITHUB_ACTIVITY))
                .thenReturn(Optional.of(CapabilitySnapshotResult.builder()
                        .response(response)
                        .etag("\"capability-etag\"")
                        .build()));

        mockMvc.perform(get("/api/v1/capabilities/github-activity")
                        .header("If-None-Match", "\"capability-etag\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"capability-etag\""))
                .andExpect(content().string(""));
    }

    @Test
    void returnsOnlyAvailableDescriptors() throws Exception {
        when(capabilityQueryService.findAvailableDescriptors()).thenReturn(List.of(CapabilityDescriptorResponse.builder()
                .capability(CapabilityType.GITHUB_ACTIVITY)
                .componentType(ComponentType.ACTIVITY_TIMELINE)
                .dataEndpoint("/api/v1/capabilities/github-activity")
                .title("Public activity")
                .sourceLabel("GitHub")
                .refreshedAt(Instant.parse("2026-08-23T12:00:00Z"))
                .build()));

        mockMvc.perform(get("/api/v1/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].capability").value("GITHUB_ACTIVITY"))
                .andExpect(jsonPath("$[0].dataEndpoint").value("/api/v1/capabilities/github-activity"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void rejectsAnUnknownCapabilityWithoutLeakingInternalDetails() throws Exception {
        mockMvc.perform(get("/api/v1/capabilities/not-a-capability"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid capability"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
