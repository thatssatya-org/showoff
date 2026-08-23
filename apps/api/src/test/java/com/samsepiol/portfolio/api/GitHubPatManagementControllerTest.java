package com.samsepiol.portfolio.api;

import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.library.token.management.TokenCreationRequest;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import com.samsepiol.portfolio.provider.CapabilitySnapshotRefreshRequest;
import com.samsepiol.portfolio.provider.GitHubActivitySnapshotStrategy;
import com.samsepiol.portfolio.security.TailnetManagementAccess;
import com.samsepiol.portfolio.security.GitHubPatManagementRequestFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {GitHubPatManagementController.class, GitHubActivityRefreshSupportController.class})
@Import({ApiExceptionHandler.class, GitHubPatManagementControllerTest.TestConfig.class})
@TestPropertySource(properties = {"portfolio.github-token.enabled=true", "portfolio.github-refresh.enabled=true"})
class GitHubPatManagementControllerTest {
    private static final String TOKEN = "github_pat_test_value_not_a_credential";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenManagementService tokenManagementService;

    @Autowired
    private GitHubActivitySnapshotStrategy gitHubActivitySnapshotStrategy;

    @BeforeEach
    void resetService() {
        reset(tokenManagementService);
        reset(gitHubActivitySnapshotStrategy);
    }

    @Test
    void persistsOnlyThroughTheServerOwnedContextAndNeverReturnsTheToken() throws Exception {
        when(tokenManagementService.create(any(), any(), any())).thenReturn(null);

        mockMvc.perform(managementPost("100.64.12.34", "{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(TOKEN))));

        var request = ArgumentCaptor.forClass(TokenCreationRequest.class);
        var storageContext = ArgumentCaptor.forClass(TokenStorageContext.class);
        var authorization = ArgumentCaptor.forClass(ManagementAuthorizationRequest.class);
        verify(tokenManagementService).create(request.capture(), storageContext.capture(), authorization.capture());
        assertThat(request.getValue().tokenCopy()).containsExactly(TOKEN.toCharArray());
        assertThat(storageContext.getValue()).isEqualTo(TokenStorageContext.builder()
                .reference(new TokenReference("portfolio", "github", "personal-access-token"))
                .keyId("github-token-v1").build());
        assertThat(authorization.getValue().getPrincipalId()).isEqualTo("tailnet:100.64.12.34");
        assertThat(authorization.getValue().getAttributes()).isEmpty();
    }

    @Test
    void deniesNonTailnetCallersWithoutPassingTokenMaterialToTheLibrary() throws Exception {
        mockMvc.perform(managementPost("203.0.113.44", "{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(TOKEN))));

        verify(tokenManagementService, never()).create(any(), any(), any());
    }

    @Test
    void rejectsMissingOrAdditionalJsonFieldsWithoutLeakingTheRequest() throws Exception {
        mockMvc.perform(managementPost("100.64.12.34", "{\"token\":\"" + TOKEN
                        + "\",\"provider\":\"attacker-specified\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("attacker-specified"))));
        verify(tokenManagementService, never()).create(any(), any(), any());
    }

    @Test
    void exposesNoCredentialReadEndpoint() throws Exception {
        mockMvc.perform(get("/internal/v1/provider-profiles/github/pat"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(TOKEN))));
    }

    @Test
    void rejectsAnOversizedBodyBeforeItCanReachTheTokenManagementService() throws Exception {
        var oversizedToken = "x".repeat(GitHubPatManagementRequestFilter.MAX_BODY_BYTES);

        mockMvc.perform(managementPost("100.64.12.34", "{\"token\":\"" + oversizedToken + "\"}"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(tokenManagementService, never()).create(any(), any(), any());
    }

    @Test
    void refreshesGitHubActivityOnlyForTailnetSupportCallers() throws Exception {
        mockMvc.perform(managementPost("/internal/v1/provider-profiles/github/activity/refresh", "100.64.12.34", ""))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));

        var request = ArgumentCaptor.forClass(CapabilitySnapshotRefreshRequest.class);
        verify(gitHubActivitySnapshotStrategy).refresh(request.capture());
        assertThat(request.getValue().getCapability()).isEqualTo(com.samsepiol.portfolio.domain.CapabilityType.GITHUB_ACTIVITY);
    }

    @Test
    void deniesNonTailnetSupportRefreshCallers() throws Exception {
        mockMvc.perform(managementPost("/internal/v1/provider-profiles/github/activity/refresh", "203.0.113.44", ""))
                .andExpect(status().isForbidden());

        verify(gitHubActivitySnapshotStrategy, never()).refresh(any());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder managementPost(
            String remoteAddress, String content) {
        return managementPost("/internal/v1/provider-profiles/github/pat", remoteAddress, content);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder managementPost(
            String path, String remoteAddress, String content) {
        return post(path)
                .contentType("application/json")
                .content(content)
                .with(request -> {
                    request.setRemoteAddr("172.30.0.2");
                    request.addHeader(TailnetManagementAccess.CANONICAL_CLIENT_ADDRESS_HEADER, remoteAddress);
                    return request;
                });
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TokenManagementService tokenManagementService() {
            return org.mockito.Mockito.mock(TokenManagementService.class);
        }

        @Bean
        @Primary
        GitHubTokenProperties gitHubTokenProperties() {
            return new GitHubTokenProperties("github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                    List.of("172.30.0.0/24"), List.of("100.64.0.0/10"));
        }

        @Bean
        TailnetManagementAccess tailnetManagementAccess(GitHubTokenProperties properties) {
            return new TailnetManagementAccess(properties);
        }

        @Bean
        GitHubPatManagementRequestFilter gitHubPatManagementRequestFilter(TailnetManagementAccess access) {
            return new GitHubPatManagementRequestFilter(access);
        }

        @Bean
        GitHubActivitySnapshotStrategy gitHubActivitySnapshotStrategy() {
            return org.mockito.Mockito.mock(GitHubActivitySnapshotStrategy.class);
        }
    }
}
