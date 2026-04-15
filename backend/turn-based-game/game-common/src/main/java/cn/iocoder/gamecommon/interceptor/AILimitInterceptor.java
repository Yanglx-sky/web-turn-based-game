package cn.iocoder.gamecommon.interceptor;

import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamecommon.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AI 调用限流拦截器
 * 每个用户每日最多调用 AI 20 次
 * 使用 Redis 实现计数
 * 超过次数直接拒绝
 */
@Component
public class AILimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JwtUtil jwtUtil;

    // 每日AI调用次数限制
    private static final int DAILY_LIMIT = 20;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        // 只拦截AI相关接口
        String path = request.getRequestURI();
        if (!path.contains("/api/ai")) {
            return true;
        }

        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return true; // 登录拦截器会处理
        }

        // 去除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 解析token获取用户ID
        Long userId = null;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return true; // 登录拦截器会处理
        }

        if (userId == null) {
            return true; // 登录拦截器会处理
        }

        // 检查AI调用次数是否超过限制
        if (!checkAILimit(userId)) {
            response.setStatus(400);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 400, \"message\": \"今日AI调用次数已达上限\", \"data\": null}");
            return false;
        }

        return true;
    }

    /**
     * 检查AI调用次数是否超过限制
     * @param userId 用户ID
     * @return 是否可以调用
     */
    private boolean checkAILimit(long userId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "ai:limit:" + userId + ":" + date;
        
        // 获取当前调用次数
        Object value = redisUtil.get(key);
        int count = 0;
        if (value != null) {
            count = Integer.parseInt(value.toString());
        }
        
        // 检查是否超过限制
        if (count >= DAILY_LIMIT) {
            return false;
        }
        
        // 自增并设置过期时间（当天结束）
        redisUtil.increment(key);
        // 计算当天剩余秒数作为过期时间
        long secondsUntilMidnight = LocalDate.now().plusDays(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) - System.currentTimeMillis() / 1000;
        redisUtil.expire(key, (int) secondsUntilMidnight);
        
        return true;
    }
}