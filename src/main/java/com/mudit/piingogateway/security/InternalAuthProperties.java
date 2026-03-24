package com.mudit.piingogateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pinggo.security.internal")
public record InternalAuthProperties(String gatewaySecret) {
}
