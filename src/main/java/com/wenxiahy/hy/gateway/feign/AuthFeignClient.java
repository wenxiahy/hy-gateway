package com.wenxiahy.hy.gateway.feign;

import com.wenxiahy.hy.common.bean.auth.AuthenticationUser;
import com.wenxiahy.hy.common.support.HyResponse;
import com.wenxiahy.hy.gateway.feign.hystrix.AuthFeignClientHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author zhouw
 * @Description
 * @Date 2020-12-21
 */
@FeignClient(name = "hy-auth-server", path = "/auth/v1", configuration = FeignConfigure.class, fallback = AuthFeignClientHystrix.class)
public interface AuthFeignClient {

    @PostMapping("/valid")
    HyResponse<AuthenticationUser> valid(@RequestParam("token") String token);
}
