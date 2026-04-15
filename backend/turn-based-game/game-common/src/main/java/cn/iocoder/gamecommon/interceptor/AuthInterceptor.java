package cn.iocoder.gamecommon.interceptor;

import cn.iocoder.gamecommon.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口鉴权拦截器
 * 基于用户身份校验接口权限
 * 防止越权访问他人角色
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
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

        // 检查是否越权访问他人资源
        String path = request.getRequestURI();
        
        // 检查URL中是否包含用户ID参数
        if (path.contains("/api/elf/") || path.contains("/api/user-elf/") || 
            path.contains("/api/battle/") || path.contains("/api/level/") || 
            path.contains("/api/achievement/") || path.contains("/api/rank/")) {
            
            // 从请求参数中获取目标用户ID
            String targetUserIdStr = request.getParameter("userId");
            if (targetUserIdStr != null) {
                try {
                    Long targetUserId = Long.parseLong(targetUserIdStr);
                    if (!userId.equals(targetUserId)) {
                        // 越权访问，返回403
                        response.setStatus(403);
                        response.setContentType("application/json; charset=utf-8");
                        response.getWriter().write("{\"code\": 403, \"message\": \"无权限访问他人资源\", \"data\": null}");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // 参数格式错误，继续执行，后续会有其他校验
                }
            }
        }

        return true;
    }
}