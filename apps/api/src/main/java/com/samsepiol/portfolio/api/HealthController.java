package com.samsepiol.portfolio.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping({"/healthz", "/api/v1/healthz"})
    public ResponseEntity<HealthResponse> healthz() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(HealthResponse.builder().status("ok").build());
    }
}
