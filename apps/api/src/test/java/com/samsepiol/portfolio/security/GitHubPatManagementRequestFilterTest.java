package com.samsepiol.portfolio.security;

import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubPatManagementRequestFilterTest {
    @Test
    void rejectsAnUnknownLengthOversizedBodyBeforeTheFilterChainCanReachMvc() throws Exception {
        var filter = new GitHubPatManagementRequestFilter(new TailnetManagementAccess(new GitHubTokenProperties(
                "github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", List.of("172.30.0.0/24"),
                List.of("100.64.0.0/10"))));
        var request = new UnknownLengthMockHttpServletRequest("POST", "/internal/v1/provider-profiles/github/pat");
        request.setRemoteAddr("172.30.0.2");
        request.addHeader(TailnetManagementAccess.CANONICAL_CLIENT_ADDRESS_HEADER, "100.64.12.34");
        request.addHeader("Transfer-Encoding", "chunked");
        request.setContent(("{\"token\":\"" + "x".repeat(GitHubPatManagementRequestFilter.MAX_BODY_BYTES) + "\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        var reachedMvc = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> reachedMvc.set(true));

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(reachedMvc).isFalse();
    }

    @Test
    void rejectsAClaimedTailnetAddressWhenTheCallerIsNotTheTrustedProxyBeforeReadingTheBody() throws Exception {
        var filter = new GitHubPatManagementRequestFilter(new TailnetManagementAccess(new GitHubTokenProperties(
                "github-token-v1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", List.of("172.30.0.0/24"),
                List.of("100.64.0.0/10"))));
        var request = new MockHttpServletRequest("POST", "/internal/v1/provider-profiles/github/pat");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader(TailnetManagementAccess.CANONICAL_CLIENT_ADDRESS_HEADER, "100.64.12.34");
        request.setContent("{\"token\":\"must-not-be-read\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        var reachedMvc = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> reachedMvc.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(reachedMvc).isFalse();
    }

    private static final class UnknownLengthMockHttpServletRequest extends MockHttpServletRequest {
        private UnknownLengthMockHttpServletRequest(String method, String requestUri) {
            super(method, requestUri);
        }

        @Override
        public long getContentLengthLong() {
            return -1;
        }
    }
}
