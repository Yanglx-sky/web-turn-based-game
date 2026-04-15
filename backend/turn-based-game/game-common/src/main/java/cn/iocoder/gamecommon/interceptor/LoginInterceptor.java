package cn.iocoder.gamecommon.interceptor;

import cn.iocoder.gamecommon.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 拦截所有需要登录的接口（角色、战斗、关卡、AI）
 * 未登录返回 401 无权限
 * 放行登录、注册、验证码接口
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        // 放行登录、注册、验证码接口
        String path = request.getRequestURI();
        if (path.contains("/users/login") || path.contains("/users/register") || path.contains("/users/captcha")) {
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

        return true;
    }
}