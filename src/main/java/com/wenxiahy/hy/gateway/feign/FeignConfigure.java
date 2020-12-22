package com.wenxiahy.hy.gateway.feign;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author zhouw
 * @Description
 * @Date 2020-12-21
 */
@Configuration
public class FeignConfigure {

    @Bean
    public Request.Options options() {
        return new Request.Options(6000, 10000);
    }

    /**
     * 重试设置，周期100豪秒，每次重试后间隔时间成指数增长，最大间隔为1秒，最多请求3次（包括第一次）
     *
     * @return
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}
