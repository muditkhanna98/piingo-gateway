package com.mudit.piingogateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pinggo.security.oauth2.swagger")
public record SwaggerOAuthProperties(String authorizationUrl, String tokenUrl) {
}
