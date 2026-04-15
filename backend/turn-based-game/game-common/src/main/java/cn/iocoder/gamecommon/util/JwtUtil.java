package cn.iocoder.gamecommon.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和解析JWT令牌
 */
@Component
public class JwtUtil {

    // 密钥，实际项目中应该从配置文件读取
    // 使用Base64编码的密钥，确保是有效的Base64字符串且解码后至少32字节
    private static final String SECRET_KEY_STRING = "Z2FtZS1zZWNyZXQta2V5LWZvci10dXJuLWJhc2VkLWdhbWUtd2l0aC1oZXJtYW55LWJ5dGVz";  // 更长的Base64编码密钥
    
    // 使用Keys.hmacShaKeyFor生成安全的密钥
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
    
    // 令牌过期时间，单位：毫秒（24小时）
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    /**
     * 生成JWT令牌
     * @param userId 用户ID
     * @param claims 自定义声明
     * @return JWT令牌
     */
    public String generateToken(long userId, Map<String, Object> claims) {
        // 创建JWT令牌
        return Jwts.builder()
                // 添加自定义声明
                .setClaims(claims)
                // 设置用户ID
                .setSubject(String.valueOf(userId))
                // 设置签发时间
                .setIssuedAt(new Date())
                // 设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // 使用HS256算法签名
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                // 构建令牌
                .compact();
    }

    /**
     * 解析JWT令牌
     * @param token JWT令牌
     * @return 令牌中的声明
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                // 设置密钥
                .setSigningKey(SECRET_KEY)
                .build()
                // 解析令牌
                .parseClaimsJws(token)
                // 获取声明
                .getBody();
    }

    /**
     * 从令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 检查令牌是否过期
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // 令牌无效
            return false;
        }
    }
}