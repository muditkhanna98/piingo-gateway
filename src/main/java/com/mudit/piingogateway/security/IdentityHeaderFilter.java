package com.mudit.piingogateway.security;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    public static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";
    public static final String HEADER_USER_SUB = "X-User-Sub";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_NAME = "X-User-Name";

    private final InternalAuthProperties internalAuthProperties;

    public IdentityHeaderFilter(InternalAuthProperties internalAuthProperties) {
        this.internalAuthProperties = internalAuthProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> mutateAuthenticatedRequest(exchange, authentication))
                .defaultIfEmpty(mutateUnauthenticatedRequest(exchange))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private ServerWebExchange mutateAuthenticatedRequest(ServerWebExchange exchange, JwtAuthenticationToken authentication) {
        String preferredUsername = authentication.getToken().getClaimAsString("preferred_username");
        String fallbackEmail = authentication.getToken().getClaimAsString("email");
        String email = StringUtils.hasText(preferredUsername) ? preferredUsername : fallbackEmail;

        String name = authentication.getToken().getClaimAsString("name");
        String sub = authentication.getToken().getSubject();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    sanitizeHeaders(headers);
                    headers.set(HEADER_GATEWAY_SECRET, internalAuthProperties.gatewaySecret());
                    headers.set(HEADER_USER_SUB, sub);
                    setIfPresent(headers, HEADER_USER_EMAIL, email);
                    setIfPresent(headers, HEADER_USER_NAME, name);
                })
                .build();

        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange mutateUnauthenticatedRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(this::sanitizeHeaders)
                .build();
        return exchange.mutate().request(request).build();
    }

    private void sanitizeHeaders(HttpHeaders headers) {
        headers.remove(HttpHeaders.AUTHORIZATION);
        headers.remove(HEADER_GATEWAY_SECRET);
        headers.remove(HEADER_USER_SUB);
        headers.remove(HEADER_USER_EMAIL);
        headers.remove(HEADER_USER_NAME);
    }

    private void setIfPresent(HttpHeaders headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }
}
