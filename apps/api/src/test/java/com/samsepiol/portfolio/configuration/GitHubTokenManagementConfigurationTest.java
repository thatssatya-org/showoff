package com.samsepiol.portfolio.configuration;

import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.library.token.management.TokenCreationRequest;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.library.token.management.persistence.TokenRecord;
import com.samsepiol.library.token.management.persistence.TokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHubTokenManagementConfigurationTest {
    @Test
    void encryptsAndUsesThePatOnlyThroughTheSnapshotLibraryService() {
        var configuration = new GitHubTokenManagementConfiguration();
        var properties = new GitHubTokenProperties("github-token-v1",
                Base64.getEncoder().encodeToString(new byte[32]), List.of("100.64.0.0/10"));
        var repository = mock(TokenRepository.class);
        var record = ArgumentCaptor.forClass(TokenRecord.class);
        when(repository.upsert(record.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = configuration.gitHubTokenManagementService(
                configuration.gitHubTokenEnvelopeCipher(configuration.gitHubTokenCredentialKeyResolver(properties)), repository,
                configuration.gitHubTokenManagementAuthorizationBoundary());
        var context = new TokenStorageContext(new TokenReference("portfolio", "github", "personal-access-token"),
                properties.keyId());
        var authorization = new ManagementAuthorizationRequest("tailnet:100.64.12.34",
                GitHubTokenManagementConfiguration.GITHUB_TOKEN_WRITE_OPERATION, Map.of());

        var receipt = service.create(new TokenCreationRequest("github_pat_test_value_not_a_credential"), context, authorization);

        assertThat(receipt.keyId()).isEqualTo("github-token-v1");
        assertThat(Base64.getEncoder().encodeToString(record.getValue().getCiphertext()))
                .doesNotContain("github_pat_test_value_not_a_credential");
        when(repository.find(context.reference())).thenReturn(Optional.of(record.getValue()));
        var decryptedToken = service.<String>useForInternalIntegration(context, authorization, chars -> new String(chars));
        assertThat(decryptedToken)
                .isEqualTo("github_pat_test_value_not_a_credential");
    }
}
