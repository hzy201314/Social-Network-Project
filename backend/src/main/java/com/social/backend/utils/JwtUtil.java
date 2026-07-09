package com.social.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // 密钥（实际项目中应该放在配置文件中）
    private static final String SECRET = "socialNetworkProjectSecretKey2026ForJWT!@#$";
    
    // 过期时间（7天）
    private static final long EXPIRATION = 1000 * 60 * 60 * 24 * 7;

    // 生成密钥
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // 生成 Token
    public String generateToken(Long userId, String username, String nickname) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("nickname", nickname);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 从 Token 中获取所有 Claims
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 从 Token 中获取用户 ID
    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    // 从 Token 中获取用户名
    public String getUsername(String token) {
        return getClaims(token).get("username", String.class);
    }

    // 从 Token 中获取昵称
    public String getNickname(String token) {
        return getClaims(token).get("nickname", String.class);
    }

    // 验证 Token 是否有效
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 从 Token 中获取过期时间
    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }
}