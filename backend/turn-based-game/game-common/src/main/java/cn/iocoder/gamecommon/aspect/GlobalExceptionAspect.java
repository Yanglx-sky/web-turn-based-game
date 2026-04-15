package cn.iocoder.gamecommon.aspect;

import cn.iocoder.gamecommon.result.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 全局异常AOP
 * 捕获所有Controller异常
 * 统一返回格式
 * 记录错误日志
 */
@Aspect
@Component
public class GlobalExceptionAspect {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionAspect.class);

    // 切入点：所有Controller方法
    @Pointcut("execution(* *..controller.*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) {
        try {
            // 执行目标方法
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 记录错误日志
            logger.error("Controller error: {}", throwable.getMessage(), throwable);
            // 统一返回错误格式
            return Result.error("服务器内部错误: " + throwable.getMessage());
        }
    }
}