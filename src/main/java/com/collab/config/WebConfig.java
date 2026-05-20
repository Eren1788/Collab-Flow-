package com.collab.config;

import com.collab.common.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册测拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/**")
                //放行接口
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        // knife4j的路径
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }
}
