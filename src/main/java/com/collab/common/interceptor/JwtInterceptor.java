package com.collab.common.interceptor;

import com.collab.common.utils.JwtUtils;
import com.collab.common.utils.LoginUserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

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

        //2.判断 Authorization 是否为空
        if (header == null || header.isEmpty()) {
            log.warn("请求头 Authorization 为空");
            throw new JwtException("请先登录，Authorization 不能为空");
        }

        //3.判断是否以 "Bearer " 开头（注意 Bearer 后面有空格）
        if (!header.startsWith("Bearer ")) {
            log.warn("Authorization 格式错误，期望格式: Bearer <token>");
            throw new JwtException("请先登录，Authorization 格式错误");
        }

        //4.提取JWT令牌，去除 "Bearer " 前缀，得到纯 JWT 令牌字符串
        String token = header.substring(7); // 去除 "Bearer " 前缀（7个字符）
        
        //5.判断令牌是否为空
        if (token == null || token.isEmpty()) {
            log.warn("JWT 令牌为空");
            throw new JwtException("请先登录，token 不能为空");
        }

        try {
            //6.解析令牌（包含签名验证、过期时间验证等）
            Claims claims = JwtUtils.parseToken(token);
            
            //7.验证令牌是否过期
            if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
                log.warn("JWT 令牌已过期");
                throw new JwtException("登录已过期，请重新登录");
            }
            
            //8.获取用户ID
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                log.warn("JWT 令牌中缺少 userId 字段");
                throw new JwtException("令牌无效，缺少用户信息");
            }
            
            Long userId = ((Number) userIdObj).longValue();
            
            //9.验证 userId 是否有效
            if (userId <= 0) {
                log.warn("JWT 令牌中的 userId 无效: {}", userId);
                throw new JwtException("令牌无效，用户信息错误");
            }

            //10.保存当前用户到 ThreadLocal
            log.info("JWT 令牌验证成功，用户ID: {}", userId);
            LoginUserContext.setUserId(userId);

            return true;
        } catch (JwtException e) {
            // JWT 相关异常直接抛出
            log.error("JWT 验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他异常包装为 JwtException
            log.error("JWT 解析异常: {}", e.getMessage());
            throw new JwtException("令牌无效或已过期，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //6.请求结束，清除ThreadLocal
        LoginUserContext.clear();
    }
}
