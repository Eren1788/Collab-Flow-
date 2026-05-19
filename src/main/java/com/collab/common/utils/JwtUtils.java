package com.collab.common.utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

/**
 * 创建 JWT 工具类
 */
public class JwtUtils {

    //密钥
    private static final String SECRET = "collab-flow-secret";
    // 过期时间（7天）
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000;

    /**
     * 生成token
     */
    public static String creatToken(Long userId,String username){
        return Jwts.builder()
                .setSubject("collab-flow")
                .claim("userId",userId)
                .claim("username",username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRE))
                .signWith(SignatureAlgorithm.ES256,SECRET)
                .compact();
    }

    /**
     * 解析token
     */
    public static Claims parseToken(String token){
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId(String token){
        Claims claims = parseToken(token);
        return ((Long) claims.get("userId")).longValue();
    }
    /**
     * 获取用户名
     */
    public static String getUsername(String token) {

        Claims claims = parseToken(token);

        return (String) claims.get("username");
    }
}
