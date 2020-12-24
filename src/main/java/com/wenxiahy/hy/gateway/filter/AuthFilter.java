package com.wenxiahy.hy.gateway.filter;

import com.wenxiahy.hy.common.bean.auth.AuthenticationUser;
import com.wenxiahy.hy.common.support.HyResponse;
import com.wenxiahy.hy.common.util.Base64Utils;
import com.wenxiahy.hy.common.util.JacksonUtils;
import com.wenxiahy.hy.common.util.Md5Utils;
import com.wenxiahy.hy.gateway.feign.AuthFeignClient;
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

    private static final String SECRET = "YOU-ARE-NOT-PREPARED";

    @Resource(type = AuthFeignClient.class)
    private AuthFeignClient authFeignClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();
        String token = request.getHeaders().getFirst("authorization");
        String timestamp = request.getHeaders().getFirst("timestamp");
        String sign = request.getHeaders().getFirst("signature");

        boolean verifySign = verifySign(path, token, timestamp, sign);
        if (!verifySign) {
            LOGGER.error("签名校验不通过，BAD REQUEST：" + request.getRemoteAddress());
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        if (path.startsWith("/hy-auth-server/")) {
            return chain.filter(exchange);
        }

        if (token == null) {
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
            String authData = Base64Utils.encryptNormalBase64(authJson);

            ServerHttpRequest newRequest = request.mutate().header("X-Auth-User", authData).build();
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

    private boolean verifySign(String path, String token, String timestamp, String sign) {
        if (timestamp == null) {
            return false;
        }

        if (sign == null) {
            return false;
        }

        try {
            long nowTs = System.currentTimeMillis();
            long signTs = Long.parseLong(timestamp);
            if ((nowTs - signTs) > 180000 || (signTs - nowTs) > 60000) {
                return false;
            }

            token = token == null ? "" : token;

            String signTemplate = "token=%s&path=%s&secret=%s&timestamp=%s&version=1";
            String str = String.format(signTemplate, token, path, SECRET, timestamp);
            String md5 = Md5Utils.string2Md5(str);

            return sign.equals(md5);
        } catch (Exception e) {
            return false;
        }
    }
}
