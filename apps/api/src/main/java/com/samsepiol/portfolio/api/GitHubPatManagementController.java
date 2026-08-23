package com.samsepiol.portfolio.api;

import com.samsepiol.library.token.management.TokenCreationRequest;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.security.TailnetManagementAccess;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "portfolio.github-token", name = "enabled", havingValue = "true")
@RequestMapping("/internal/v1/provider-profiles/github/pat")
public class GitHubPatManagementController {
    private static final TokenReference GITHUB_TOKEN_REFERENCE = new TokenReference(
            "portfolio", "github", "personal-access-token");

    private final TokenManagementService tokenManagementService;
    private final TailnetManagementAccess tailnetManagementAccess;
    private final GitHubTokenProperties tokenProperties;

    public GitHubPatManagementController(TokenManagementService tokenManagementService,
                                         TailnetManagementAccess tailnetManagementAccess,
                                         GitHubTokenProperties tokenProperties) {
        this.tokenManagementService = tokenManagementService;
        this.tailnetManagementAccess = tailnetManagementAccess;
        this.tokenProperties = tokenProperties;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> update(@RequestBody JsonNode requestBody,
                                       HttpServletRequest servletRequest) {
        var request = GitHubPatUpdateRequest.from(requestBody);
        var authorization = tailnetManagementAccess.authorize(servletRequest);
        tokenManagementService.create(TokenCreationRequest.builder().token(request.getToken()).build(), storageContext(), authorization);
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    private TokenStorageContext storageContext() {
        return TokenStorageContext.builder().reference(GITHUB_TOKEN_REFERENCE).keyId(tokenProperties.keyId()).build();
    }
}
