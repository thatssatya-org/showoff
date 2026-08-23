package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.application.CapabilityQueryService;
import com.samsepiol.portfolio.domain.CapabilityType;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/capabilities")
public class CapabilityController {
    private static final CacheControl PUBLIC_CACHE_CONTROL = CacheControl.maxAge(5, TimeUnit.MINUTES)
            .staleWhileRevalidate(1, TimeUnit.DAYS)
            .cachePublic();

    private final CapabilityQueryService capabilityQueryService;

    public CapabilityController(CapabilityQueryService capabilityQueryService) {
        this.capabilityQueryService = capabilityQueryService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<CapabilityDescriptorResponse>> capabilities() {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE_CONTROL)
                .body(capabilityQueryService.findAvailableDescriptors());
    }

    @GetMapping("/{capability}")
    public ResponseEntity<CapabilitySnapshotResponse> capability(
            @PathVariable String capability,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        var capabilityType = CapabilityType.fromApiValue(capability)
                .orElseThrow(InvalidCapabilityException::new);
        var snapshotResult = capabilityQueryService.findSnapshot(capabilityType);

        if (snapshotResult.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        var result = snapshotResult.get();
        if (result.getEtag().equals(ifNoneMatch)) {
            return ResponseEntity.status(304)
                    .cacheControl(PUBLIC_CACHE_CONTROL)
                    .eTag(result.getEtag())
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE_CONTROL)
                .eTag(result.getEtag())
                .body(result.getResponse());
    }
}
