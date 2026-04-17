package cn.iocoder.gamecommon.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间（秒）
     */
    public void set(String key, Object value, long expireTime) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 自增
     * @param key 键
     * @return 自增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param expireTime 过期时间（秒）
     */
    public void expire(String key, long expireTime) {
        redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 检查AI调用次数是否超过限制
     * @param userId 用户ID
     * @param limit 每日限制次数
     * @return 是否超过限制（true=未超过，允许调用；false=已超过，拒绝调用）
     */
    public boolean checkAILimit(long userId, int limit) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "ai:limit:" + userId + ":" + date;
        
        // 获取当前调用次数
        Object value = redisTemplate.opsForValue().get(key);
        int count = 0;
        if (value != null) {
            count = Integer.parseInt(value.toString());
        }
        
        // 检查是否超过限制
        if (count >= limit) {
            return false; // 已达上限
        }
        
        // 自增并设置过期时间（当天结束）
        redisTemplate.opsForValue().increment(key);
        // 计算当天剩余秒数作为过期时间（使用东八区）
        long secondsUntilMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(java.time.ZoneOffset.ofHours(8))
            .toEpochSecond() - System.currentTimeMillis() / 1000;
        redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        
        return true; // 未超过限制，允许调用
    }

    /**
     * 获取AI今日调用次数
     * @param userId 用户ID
     * @return 调用次数
     */
    public int getAITodayCount(long userId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "ai:limit:" + userId + ":" + date;
        
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return 0;
    }
}