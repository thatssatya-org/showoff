package com.samsepiol.portfolio.configuration;

import com.samsepiol.library.encryption.credential.AesGcmCredentialEnvelopeCipher;
import com.samsepiol.library.encryption.credential.CredentialEnvelopeCipher;
import com.samsepiol.library.encryption.credential.CredentialKeyResolver;
import com.samsepiol.library.core.security.management.ManagementAuthorizationBoundary;
import com.samsepiol.library.core.security.management.PolicyBackedManagementAuthorization;
import com.samsepiol.library.core.util.DateTimeUtils;
import com.samsepiol.library.mongo.Repository;
import com.samsepiol.library.mongo.codec.CodecSupplier;
import com.samsepiol.library.token.management.DefaultTokenManagementService;
import com.samsepiol.library.token.management.TokenReference;
import com.samsepiol.library.token.management.TokenStorageContext;
import com.samsepiol.library.token.management.api.TokenManagementRequestContext;
import com.samsepiol.library.token.management.api.TokenManagementRequestContextResolver;
import com.samsepiol.library.token.management.persistence.MongoTokenRepository;
import com.samsepiol.library.token.management.persistence.TokenManagementCodecSupplier;
import com.samsepiol.library.token.management.persistence.TokenRepository;
import com.samsepiol.portfolio.security.TailnetManagementAccess;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.LongSupplier;

@Configuration
@ConditionalOnExpression("'${portfolio.github-token.enabled:false}' == 'true' or '${portfolio.beszel.enabled:false}' == 'true'")
@EnableConfigurationProperties(GitHubTokenProperties.class)
public class GitHubTokenManagementConfiguration {
    public static final String GITHUB_TOKEN_WRITE_OPERATION = "github-token-write";
    public static final String GITHUB_TOKEN_USE_OPERATION = "github-token-use";
    public static final String BESZEL_TOKEN_WRITE_OPERATION = "beszel-token-write";
    private static final TokenReference GITHUB_TOKEN_REFERENCE = new TokenReference(
            "portfolio", "github", "personal-access-token");

    @Bean
    @Primary
    LongSupplier gitHubTokenEpochMillisSupplier() {
        return DateTimeUtils::currentEpochMillis;
    }

    @Bean
    @Primary
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
    @Primary
    CredentialEnvelopeCipher gitHubTokenEnvelopeCipher(CredentialKeyResolver gitHubTokenCredentialKeyResolver) {
        return new AesGcmCredentialEnvelopeCipher(gitHubTokenCredentialKeyResolver, DateTimeUtils::currentEpochMillis);
    }

    @Bean
    @Primary
    ManagementAuthorizationBoundary gitHubTokenManagementAuthorizationBoundary() {
        return new PolicyBackedManagementAuthorization(request ->
                (GITHUB_TOKEN_WRITE_OPERATION.equals(request.getOperation())
                        && request.getPrincipalId().startsWith("tailnet:"))
                        || (BESZEL_TOKEN_WRITE_OPERATION.equals(request.getOperation())
                        && request.getPrincipalId().startsWith("tailnet:"))
                        || (GITHUB_TOKEN_USE_OPERATION.equals(request.getOperation())
                        && "github-refresh-scheduler".equals(request.getPrincipalId())));
    }

    @Bean
    @Primary
    CodecSupplier gitHubTokenManagementCodecSupplier() {
        return new TokenManagementCodecSupplier();
    }

    @Bean
    @Primary
    TokenRepository gitHubTokenRepository(Repository repository) {
        return new MongoTokenRepository(repository);
    }

    @Bean
    @Primary
    DefaultTokenManagementService gitHubTokenManagementService(
            CredentialEnvelopeCipher gitHubTokenEnvelopeCipher,
            TokenRepository gitHubTokenRepository,
            ManagementAuthorizationBoundary gitHubTokenManagementAuthorizationBoundary) {
        return new DefaultTokenManagementService(gitHubTokenEnvelopeCipher, gitHubTokenRepository,
                gitHubTokenManagementAuthorizationBoundary);
    }

    @Bean
    TokenManagementRequestContextResolver gitHubTokenManagementRequestContextResolver(
            TailnetManagementAccess tailnetManagementAccess,
            GitHubTokenProperties properties) {
        return () -> TokenManagementRequestContext.builder()
                .storageContext(TokenStorageContext.required(GITHUB_TOKEN_REFERENCE, properties.keyId()))
                .authorizationRequest(tailnetManagementAccess.authorize(
                        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest()))
                .build();
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
