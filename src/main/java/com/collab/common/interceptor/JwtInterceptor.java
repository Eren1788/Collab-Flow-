package com.collab.common.interceptor;

import com.collab.common.utils.JwtUtils;
import com.collab.common.utils.LoginUserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 *  下一步注册拦截器-WebConfig
 */
@Slf4j
public class JwtInterceptor implements HandlerInterceptor{
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        log.info("登录拦截器，开始拦截!");
        //1.获取 Authorization
        String header = request.getHeader("Authorization");

        if (header==null || !header.startsWith("Bearer")){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //2.提取JWT令牌,除 "Bearer" 前缀，得到纯 JWT 令牌字符串
        String token = header.replace("Bearer", "");

        try {
            //3.解析令牌
            Claims claims = JwtUtils.parseToken(token);
            //4.获取id
            Long userId = ((Number) claims.get("userId")).longValue();

            //5.保存当前用户！！！
            log.info("已保存当前用户到ThreadLocal:{}",userId);
            LoginUserContext.setUserId(userId);

            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //6.请求结束，清除ThreadLocal
        LoginUserContext.clear();
    }
}
