package com.wenxiahy.hy.gateway.filter;

import com.wenxiahy.hy.common.bean.auth.AuthenticationUser;
import com.wenxiahy.hy.common.support.HyResponse;
import com.wenxiahy.hy.common.util.Base64Utils;
import com.wenxiahy.hy.common.util.HyStringUtils;
import com.wenxiahy.hy.common.util.JacksonUtils;
import com.wenxiahy.hy.gateway.feign.AuthFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @Author zhouw
 * @Description
 * @Date 2020-12-20
 */
@Component
@RestController
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    @Resource(type = AuthFeignClient.class)
    private AuthFeignClient authFeignClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/hy-auth-server/")) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        HyResponse<AuthenticationUser> authResponse = authFeignClient.valid(token);
        AuthenticationUser authUser = authResponse.getResult();
        if (authUser == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        try {
            String authJson = JacksonUtils.object2Json(authUser);
            String base64Str = Base64Utils.encryptNormalBase64(authJson);
            String xor = HyStringUtils.xorEncry(base64Str);

            ServerHttpRequest newRequest = request.mutate().header("X-Auth-User", xor).build();
            ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
            return chain.filter(newExchange);
        } catch (Exception e) {
            LOGGER.error("生成Header: X-Auth-User异常", e);
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
