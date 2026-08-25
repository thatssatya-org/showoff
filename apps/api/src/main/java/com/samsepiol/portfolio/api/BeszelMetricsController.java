package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.application.BeszelMetricsService;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.security.TailnetManagementAccess;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "portfolio.beszel", name = "enabled", havingValue = "true")
@RequestMapping("/internal/v1/provider-profiles/beszel")
public class BeszelMetricsController {
    private final BeszelMetricsService beszelMetricsService;
    private final TailnetManagementAccess tailnetManagementAccess;

    @GetMapping("/metrics")
    public ResponseEntity<BeszelMetricsResponse> metrics(HttpServletRequest servletRequest) {
        tailnetManagementAccess.authorize(servletRequest,
                GitHubTokenManagementConfiguration.BESZEL_METRICS_READ_OPERATION);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(beszelMetricsService.metrics());
    }
}
