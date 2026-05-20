package com.collab.config;

import com.collab.common.interceptor.JwtInterceptor;
import com.collab.common.interceptor.PermissionInterceptor;
import com.collab.mapper.PermissionMapper;
import com.collab.mapper.RolePermissionMapper;
import com.collab.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册RedisTemplate
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 注册权限拦截器
     */
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;


    /**
     * 注册JWT拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new JwtInterceptor(redisTemplate))
                .addPathPatterns("/**")
                // 放行接口
                .excludePathPatterns(
                        // 用户模块
                        "/user/login",
                        "/user/register",

                        // knife4j
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",

                        // 静态资源
                        "/error"
                );
        registry.addInterceptor(
                new PermissionInterceptor(
                        userRoleMapper,
                        rolePermissionMapper,
                        permissionMapper
                )
        );
    }

    /**
     * 配置跨域:
     *      前端的访问地址是：localhost:5173
     *      后端的访问地址是：localhost:8080
     *      出现跨域问题，后端要配置跨域类
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 前端地址
                .allowedOriginPatterns("*")
                // 请求方式
                .allowedMethods("*")
                // 请求头
                .allowedHeaders("*")
                // 是否携带cookie
                .allowCredentials(true)
                // 最大缓存时间
                .maxAge(3600);
    }
}