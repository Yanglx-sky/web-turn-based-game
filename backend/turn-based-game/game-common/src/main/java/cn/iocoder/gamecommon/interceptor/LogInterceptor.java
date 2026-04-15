package cn.iocoder.gamecommon.interceptor;

import cn.iocoder.gamecommon.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 全局请求日志拦截器
 * 记录请求路径、用户ID、耗时、异常
 */
@Component
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LogInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    // 存储请求开始时间
    private ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        startTime.set(System.currentTimeMillis());
        
        // 记录请求信息
        String path = request.getRequestURI();
        Long userId = getUserIdFromToken(request);
        logger.info("Request start - Path: {}, UserId: {}", path, userId);
        
        return true;
    }

    @Override
    public void postHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {
        // 可以在这里添加额外的处理逻辑
    }

    @Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 计算请求耗时
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime.get();
        
        // 记录请求结束信息
        String path = request.getRequestURI();
        Long userId = getUserIdFromToken(request);
        
        if (ex != null) {
            logger.error("Request end - Path: {}, UserId: {}, Cost: {}ms, Exception: {}", 
                path, userId, costTime, ex.getMessage());
        } else {
            logger.info("Request end - Path: {}, UserId: {}, Cost: {}ms", 
                path, userId, costTime);
        }
        
        // 清除线程本地变量
        startTime.remove();
    }

    /**
     * 从请求头中获取token并解析用户ID
     * @param request 请求对象
     * @return 用户ID
     */
    private Long getUserIdFromToken(jakarta.servlet.http.HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }

        // 去除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}