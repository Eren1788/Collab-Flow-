package com.collab.common.interceptor;

import com.collab.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 *  下一步组测拦截器
 */
public class JwtInterceptor implements HandlerInterceptor{
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        //1.获取认证头信息
        String header = request.getHeader("Authorization");

        if (header==null || !header.startsWith("Bearer")){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //2.提取JWT令牌,除 "Bearer" 前缀，得到纯 JWT 令牌字符串
        String token = header.replace("Bearer", "");

        try {
            Claims claims = JwtUtils.parseToken(token);
            request.setAttribute("userId",claims.get("userId"));
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
