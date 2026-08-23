package com.samsepiol.portfolio.configuration;

import com.samsepiol.library.core.security.credential.AesGcmCredentialEnvelopeCipher;
import com.samsepiol.library.core.security.credential.CredentialEnvelopeCipher;
import com.samsepiol.library.core.security.credential.CredentialKeyResolver;
import com.samsepiol.library.core.security.management.ManagementAuthorizationBoundary;
import com.samsepiol.library.core.security.management.PolicyBackedManagementAuthorization;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.library.mongo.codec.CodecSupplier;
import com.samsepiol.library.token.management.DefaultTokenManagementService;
import com.samsepiol.library.token.management.TokenManagementService;
import com.samsepiol.library.token.management.persistence.MongoTokenRepository;
import com.samsepiol.library.token.management.persistence.TokenManagementCodecSupplier;
import com.samsepiol.library.token.management.persistence.TokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(prefix = "portfolio.github-token", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GitHubTokenProperties.class)
public class GitHubTokenManagementConfiguration {
    public static final String GITHUB_TOKEN_WRITE_OPERATION = "github-token-write";

    @Bean
    CredentialKeyResolver gitHubTokenCredentialKeyResolver(GitHubTokenProperties properties) {
        var keyMaterial = decodeKey(properties.keyBase64());
        try {
            var key = new SecretKeySpec(keyMaterial, "AES");
            return keyId -> properties.keyId().equals(keyId) ? key : null;
        } finally {
            Arrays.fill(keyMaterial, (byte) 0);
        }
    }

    @Bean
    CredentialEnvelopeCipher gitHubTokenEnvelopeCipher(CredentialKeyResolver gitHubTokenCredentialKeyResolver) {
        return new AesGcmCredentialEnvelopeCipher(gitHubTokenCredentialKeyResolver);
    }

    @Bean
    ManagementAuthorizationBoundary gitHubTokenManagementAuthorizationBoundary() {
        return new PolicyBackedManagementAuthorization(request ->
                GITHUB_TOKEN_WRITE_OPERATION.equals(request.getOperation())
                        && request.getPrincipalId().startsWith("tailnet:"));
    }

    @Bean
    CodecSupplier gitHubTokenManagementCodecSupplier() {
        return new TokenManagementCodecSupplier();
    }

    @Bean
    TokenRepository gitHubTokenRepository(Repository repository) {
        return new MongoTokenRepository(repository);
    }

    @Bean
    TokenManagementService gitHubTokenManagementService(
            CredentialEnvelopeCipher gitHubTokenEnvelopeCipher,
            TokenRepository gitHubTokenRepository,
            ManagementAuthorizationBoundary gitHubTokenManagementAuthorizationBoundary) {
        return new DefaultTokenManagementService(gitHubTokenEnvelopeCipher, gitHubTokenRepository,
                gitHubTokenManagementAuthorizationBoundary);
    }

    private static byte[] decodeKey(String encodedKey) {
        try {
            var keyMaterial = Base64.getDecoder().decode(encodedKey);
            if (keyMaterial.length != 32) {
                Arrays.fill(keyMaterial, (byte) 0);
                throw new IllegalArgumentException("portfolio.github-token.key-base64 must decode to exactly 32 bytes");
            }
            return keyMaterial;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("portfolio.github-token.key-base64 is invalid", exception);
        }
    }
}
