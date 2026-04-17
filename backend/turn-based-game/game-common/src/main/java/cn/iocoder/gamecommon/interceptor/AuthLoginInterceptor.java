package cn.iocoder.gamecommon.interceptor;

import cn.iocoder.gamecommon.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录和鉴权拦截器
 * 合并了原来的LoginInterceptor和AuthInterceptor的功能
 * 拦截所有需要登录的接口（角色、战斗、关卡、AI）
 * 未登录返回 401 无权限
 * 放行登录、注册、验证码接口
 */
@Slf4j
@Component
public class AuthLoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        // 放行登录、注册、验证码接口
        String path = request.getRequestURI();
        if (path.contains("/users/login") || path.contains("/users/register") || path.contains("/users/captcha") || 
            path.contains("/levels") || path.contains("/level/") || 
            path.contains("/elf/") || path.contains("/user-elf/") || 
            path.contains("/battle/ai/strategy") || path.contains("/users/me/level-stars") || 
            path.contains("/friends/") || path.contains("/equip/") || 
            path.contains("/equips/") || path.contains("/potion/") || 
            path.contains("/potions/") || path.contains("/shop/") || 
            path.contains("/train/") || path.contains("/rank/") || 
            path.contains("/achievement/") || path.contains("/ai/")) {
            return true;
        }

        // 从请求头中获取token
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            // 未登录，返回401
            response.setStatus(401);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 401, \"message\": \"未登录，无权限访问\", \"data\": null}");
            return false;
        }

        // 去除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证token
        try {
            if (!jwtUtil.validateToken(token)) {
                // token无效，返回401
                response.setStatus(401);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"登录已过期，请重新登录\", \"data\": null}");
                return false;
            }
        } catch (Exception e) {
            // token解析失败，返回401
            response.setStatus(401);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 401, \"message\": \"登录已过期，请重新登录\", \"data\": null}");
            return false;
        }

        // 解析token获取用户ID
        Long userId = null;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            // token解析失败，返回401
            response.setStatus(401);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 401, \"message\": \"登录已过期，请重新登录\", \"data\": null}");
            return false;
        }

        if (userId == null) {
            // token解析失败，返回401
            response.setStatus(401);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 401, \"message\": \"登录已过期，请重新登录\", \"data\": null}");
            return false;
        }

        // 检查是否越权访问他人资源
        if (path.contains("/elf/") || path.contains("/user-elf/") || 
            path.contains("/battle/") || path.contains("/level/") || 
            path.contains("/achievement/") || path.contains("/rank/") ||
            path.contains("/users/")) {
            
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