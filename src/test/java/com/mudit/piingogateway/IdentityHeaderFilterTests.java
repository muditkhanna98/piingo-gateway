package com.mudit.piingogateway;

import com.mudit.piingogateway.security.IdentityHeaderFilter;
import com.mudit.piingogateway.security.InternalAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdentityHeaderFilterTests {

    @Test
    void authenticatedRequestGetsTrustedHeaders() {
        IdentityHeaderFilter filter = new IdentityHeaderFilter(new InternalAuthProperties("test-gateway-secret"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-123")
                .claim("preferred_username", "alice@example.com")
                .claim("name", "Alice")
                .build(), AuthorityUtils.NO_AUTHORITIES);

        ServerWebExchange exchange = exchangeWithPrincipal(authentication, MockServerHttpRequest.get("/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer should-be-removed")
                .header(IdentityHeaderFilter.HEADER_USER_SUB, "spoofed-sub")
                .header(IdentityHeaderFilter.HEADER_USER_EMAIL, "spoofed@example.com")
                .header(IdentityHeaderFilter.HEADER_GATEWAY_SECRET, "spoofed-secret")
                .build());

        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        filter.filter(exchange, mutatedExchange -> {
            forwardedRequest.set(mutatedExchange.getRequest());
            return Mono.empty();
        }).block();

        assertEquals("subject-123", forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_USER_SUB));
        assertEquals("alice@example.com", forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_USER_EMAIL));
        assertEquals("Alice", forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_USER_NAME));
        assertEquals("test-gateway-secret", forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_GATEWAY_SECRET));
        assertNull(forwardedRequest.get().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void unauthenticatedRequestHasSpoofedHeadersRemoved() {
        IdentityHeaderFilter filter = new IdentityHeaderFilter(new InternalAuthProperties("test-gateway-secret"));
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer should-be-removed")
                .header(IdentityHeaderFilter.HEADER_USER_SUB, "spoofed-sub")
                .header(IdentityHeaderFilter.HEADER_USER_EMAIL, "spoofed@example.com")
                .header(IdentityHeaderFilter.HEADER_GATEWAY_SECRET, "spoofed-secret")
                .build());

        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        filter.filter(exchange, mutatedExchange -> {
            forwardedRequest.set(mutatedExchange.getRequest());
            return Mono.empty();
        }).block();

        assertNull(forwardedRequest.get().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertNull(forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_USER_SUB));
        assertNull(forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_USER_EMAIL));
        assertNull(forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.HEADER_GATEWAY_SECRET));
    }

    private ServerWebExchange exchangeWithPrincipal(JwtAuthenticationToken authentication, MockServerHttpRequest request) {
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        return new ServerWebExchangeDecorator(exchange) {
            @Override
            public <T extends java.security.Principal> Mono<T> getPrincipal() {
                return Mono.just((T) authentication);
            }
        };
    }
}
