package com.samsepiol.portfolio.security;

import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${portfolio.github-token.enabled:false}' == 'true' or '${portfolio.beszel.enabled:false}' == 'true'")
public class TailnetManagementAccess {
    public static final String CANONICAL_CLIENT_ADDRESS_HEADER = "X-Portfolio-Client-Address";

    private final List<CidrRange> trustedProxyRanges;
    private final List<CidrRange> tailnetRanges;

    public TailnetManagementAccess(GitHubTokenProperties properties) {
        this.trustedProxyRanges = properties.trustedProxyCidrs().stream().map(CidrRange::parse).toList();
        this.tailnetRanges = properties.tailnetCidrs().stream().map(CidrRange::parse).toList();
    }

    public ManagementAuthorizationRequest authorize(HttpServletRequest request) {
        return authorize(request, GitHubTokenManagementConfiguration.GITHUB_TOKEN_WRITE_OPERATION);
    }

    public ManagementAuthorizationRequest authorize(HttpServletRequest request, String operation) {
        if (!isTrustedProxyAddress(request.getRemoteAddr())) {
            throw new ManagementAccessDeniedException();
        }
        var clientAddress = canonicalClientAddress(request.getHeader(CANONICAL_CLIENT_ADDRESS_HEADER));
        if (clientAddress == null || !isTailnetAddress(clientAddress)) {
            throw new ManagementAccessDeniedException();
        }
        return ManagementAuthorizationRequest.builder().principalId("tailnet:" + clientAddress)
                .operation(operation).attributes(Map.of()).build();
    }

    private boolean isTrustedProxyAddress(String remoteAddress) {
        return isInRange(remoteAddress, trustedProxyRanges);
    }

    private boolean isTailnetAddress(String remoteAddress) {
        return isInRange(remoteAddress, tailnetRanges);
    }

    private static boolean isInRange(String remoteAddress, List<CidrRange> ranges) {
        try {
            var address = InetAddress.getByName(remoteAddress).getAddress();
            return ranges.stream().anyMatch(range -> range.contains(address));
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private static String canonicalClientAddress(String value) {
        if (value == null || value.isBlank()
                || !(value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}") || value.matches("[0-9A-Fa-f:]+"))) {
            return null;
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private record CidrRange(byte[] network, int prefixLength) {
        static CidrRange parse(String value) {
            var separator = value.lastIndexOf('/');
            if (separator <= 0 || separator == value.length() - 1) {
                throw new IllegalArgumentException("portfolio.github-token.tailnet-cidrs must contain CIDR values");
            }
            try {
                var network = InetAddress.getByName(value.substring(0, separator)).getAddress();
                var prefixLength = Integer.parseInt(value.substring(separator + 1));
                if (prefixLength < 0 || prefixLength > network.length * Byte.SIZE) {
                    throw new IllegalArgumentException("portfolio.github-token.tailnet-cidrs contains an invalid prefix");
                }
                return new CidrRange(network, prefixLength);
            } catch (UnknownHostException | NumberFormatException exception) {
                throw new IllegalArgumentException("portfolio.github-token.tailnet-cidrs contains an invalid CIDR", exception);
            }
        }

        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            var completeBytes = prefixLength / Byte.SIZE;
            var remainingBits = prefixLength % Byte.SIZE;
            for (var index = 0; index < completeBytes; index++) {
                if (address[index] != network[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            var mask = 0xFF << (Byte.SIZE - remainingBits);
            return (address[completeBytes] & mask) == (network[completeBytes] & mask);
        }
    }
}
