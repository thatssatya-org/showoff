package com.samsepiol.portfolio.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.samsepiol.library.token.management.TokenCreationRequest;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.security.TailnetManagementAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write-only Tailnet pairing boundary. Beszel is never proxied to a browser;
 * the backend later consumes this encrypted token through its provider adapter.
 */
@RestController
@ConditionalOnBean(TokenManagementService.class)
@ConditionalOnProperty(prefix = "portfolio.beszel", name = "enabled", havingValue = "true")
@RequestMapping("/internal/v1/provider-profiles/beszel/pair")
public class BeszelPairingController {
    private static final TokenReference BESZEL_TOKEN_REFERENCE = new TokenReference(
            "portfolio", "beszel", "api-token");

    private final TokenManagementService tokenManagementService;
    private final TailnetManagementAccess tailnetManagementAccess;
    private final GitHubTokenProperties tokenProperties;

    public BeszelPairingController(TokenManagementService tokenManagementService,
                                   TailnetManagementAccess tailnetManagementAccess,
                                   GitHubTokenProperties tokenProperties) {
        this.tokenManagementService = tokenManagementService;
        this.tailnetManagementAccess = tailnetManagementAccess;
        this.tokenProperties = tokenProperties;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> pair(@RequestBody JsonNode requestBody, HttpServletRequest servletRequest) {
        var request = BeszelPairingRequest.from(requestBody);
        tokenManagementService.create(TokenCreationRequest.builder().token(request.getToken()).build(), storageContext(),
                tailnetManagementAccess.authorize(servletRequest,
                        GitHubTokenManagementConfiguration.BESZEL_TOKEN_WRITE_OPERATION));
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(BESZEL_TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }
}
