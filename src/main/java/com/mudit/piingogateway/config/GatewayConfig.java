package com.mudit.piingogateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return route("user_route")
                .route(path("/api/users/**"), http())
                .before(rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .filter(lb("piingo-user-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> chatServiceRoute() {
        return route("conversation_route")
                .route(path("/api/conversations/**"), http())
                .before(rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .filter(lb("piingo-chat-service"))
                .build();
    }

}
