package cn.iocoder.gamecommon.aspect;

import cn.iocoder.gamecommon.annotation.RequireLogin;
import cn.iocoder.gamecommon.result.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 接口鉴权AOP
 * 校验用户是否登录
 * 未登录直接返回401
 * 仅作用于添加了@RequireLogin注解的接口
 */
@Aspect
@Component
public class AuthAspect {

    // 切入点：所有添加了@RequireLogin注解的方法或类
    @Pointcut("@annotation(cn.iocoder.gamecommon.annotation.RequireLogin) || @within(cn.iocoder.gamecommon.annotation.RequireLogin)")
    public void requireLoginPointcut() {
    }

    @Around("requireLoginPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 从方法参数中获取用户ID
        Long userId = null;
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                // 假设第一个Long类型参数是userId
                userId = (Long) arg;
                break;
            }
        }

        if (userId == null) {
            // 未登录，返回401
            return Result.error("未登录，无权限访问");
        }
        // 已登录，执行目标方法
        return joinPoint.proceed();
    }
}