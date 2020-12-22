package com.wenxiahy.hy.gateway.feign.hystrix;

import com.wenxiahy.hy.common.bean.auth.AuthenticationUser;
import com.wenxiahy.hy.common.support.HyResponse;
import com.wenxiahy.hy.common.support.ResponseMapper;
import com.wenxiahy.hy.gateway.feign.AuthFeignClient;
import org.springframework.stereotype.Component;

/**
 * @Author zhouw
 * @Description
 * @Date 2020-12-21
 */
@Component
public class AuthFeignClientHystrix implements AuthFeignClient {

    @Override
    public HyResponse<AuthenticationUser> valid(String token) {
        return ResponseMapper.error("HY-AUTH-SERVER Unavailable");
    }
}
