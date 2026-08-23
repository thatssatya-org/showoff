package com.samsepiol.portfolio.security;

import com.samsepiol.library.core.security.management.ManagementAuthorizationRequest;
import com.samsepiol.portfolio.configuration.GitHubTokenManagementConfiguration;
import com.samsepiol.portfolio.configuration.GitHubTokenProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "portfolio.github-token", name = "enabled", havingValue = "true")
public class TailnetManagementAccess {
    private final List<CidrRange> tailnetRanges;

    public TailnetManagementAccess(GitHubTokenProperties properties) {
        this.tailnetRanges = properties.tailnetCidrs().stream().map(CidrRange::parse).toList();
    }

    public ManagementAuthorizationRequest authorize(HttpServletRequest request) {
        var remoteAddress = request.getRemoteAddr();
        if (!isTailnetAddress(remoteAddress)) {
            throw new ManagementAccessDeniedException();
        }
        return new ManagementAuthorizationRequest("tailnet:" + remoteAddress,
                GitHubTokenManagementConfiguration.GITHUB_TOKEN_WRITE_OPERATION, Map.of());
    }

    private boolean isTailnetAddress(String remoteAddress) {
        try {
            var address = InetAddress.getByName(remoteAddress).getAddress();
            return tailnetRanges.stream().anyMatch(range -> range.contains(address));
        } catch (UnknownHostException exception) {
            return false;
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
