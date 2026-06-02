package com.collab.config;

import com.collab.common.interceptor.JwtInterceptor;
import com.collab.common.interceptor.PermissionInterceptor;
import com.collab.mapper.PermissionMapper;
import com.collab.mapper.RolePermissionMapper;
import com.collab.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(redisTemplate))
                .addPathPatterns("/**")
                // 放行接口
                .excludePathPatterns(
                        // 用户模块
                        "/user/login",
                        "/user/register",

                        // Logo 公开接口（获取 URL 无需登录）
                        "/logo/url",
                        "/logo/image",

                        // knife4j
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",

                        // 静态资源
                        "/error",
                        "/uploads/**"
                );
        registry.addInterceptor(
                new PermissionInterceptor(
                        userRoleMapper,
                        rolePermissionMapper,
                        permissionMapper
                )
        );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadPath + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}