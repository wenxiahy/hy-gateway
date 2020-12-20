package com.wenxiahy.hy.gateway.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Date;

/**
 * @Author zhouw
 * @Description
 * @Date 2020-12-20
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/hy-auth-server/")) {
            return chain.filter(exchange);
        }

        ServerHttpResponse response = exchange.getResponse();
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank(token)) {
            return authError(response, path);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> authError(ServerHttpResponse response, String path) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"timestamp\": \"").append(new Date()).append("\",");
        sb.append("\"path\": \"").append(path).append("\",");
        sb.append("\"status\": 404,");
        sb.append("\"error\": \"Unauthorized\"");
        sb.append("}");

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        DataBuffer buffer = response.bufferFactory().wrap(sb.toString().getBytes(Charset.forName("UTF-8")));

        return response.writeWith(Flux.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
