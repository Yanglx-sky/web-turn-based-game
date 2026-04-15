package cn.iocoder.gamecommon.aspect;

import cn.iocoder.gamecommon.annotation.Loggable;
import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 全局请求日志AOP
 * 记录请求路径、用户ID、耗时、异常
 * 支持@Loggable注解，提供详细日志记录
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    // 切入点：所有Controller方法
    @Pointcut("execution(* *..controller.*.*(..))")
    public void controllerPointcut() {
    }

    // 切入点：所有添加了@Loggable注解的方法或类
    @Pointcut("@annotation(cn.iocoder.gamecommon.annotation.Loggable) || @within(cn.iocoder.gamecommon.annotation.Loggable)")
    public void loggablePointcut() {
    }

    @Around("controllerPointcut() || loggablePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getName();
        
        // 检查是否有@Loggable注解
        Loggable loggable = method.getAnnotation(Loggable.class);
        if (loggable == null) {
            // 检查类级别注解
            loggable = joinPoint.getTarget().getClass().getAnnotation(Loggable.class);
        }
        
        // 获取日志描述
        String description = loggable != null && !loggable.value().isEmpty() ? loggable.value() : methodName;
        
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
        
        // 记录请求开始
        if (loggable != null) {
            logger.info("[Loggable] Start - {} - Class: {}, Method: {}, UserId: {}", 
                description, className, methodName, userId);
            
            // 记录请求参数
            if (loggable.recordParams()) {
                try {
                    // 过滤掉无法序列化的参数，如HttpServletRequest、HttpServletResponse等
                    Object[] serializableArgs = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            String paramClassName = args[i].getClass().getName();
                            if (paramClassName.startsWith("jakarta.servlet.http.") || paramClassName.startsWith("javax.servlet.http.")) {
                                serializableArgs[i] = paramClassName.substring(paramClassName.lastIndexOf(".") + 1) + " [filtered]";
                            } else {
                                try {
                                    // 尝试序列化，避免其他无法序列化的类型
                                    JSON.toJSONString(args[i]);
                                    serializableArgs[i] = args[i];
                                } catch (Exception e) {
                                    serializableArgs[i] = args[i].getClass().getSimpleName() + " [filtered]";
                                }
                            }
                        } else {
                            serializableArgs[i] = null;
                        }
                    }
                    logger.info("[Loggable] Params - {}: {}", description, JSON.toJSONString(serializableArgs));
                } catch (Exception e) {
                    logger.warn("[Loggable] Failed to serialize params: {}", e.getMessage());
                }
            }
        } else {
            logger.info("Request start - Class: {}, Method: {}, UserId: {}", className, methodName, userId);
        }
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        Object result = null;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录返回值
            if (loggable != null && loggable.recordResult()) {
                try {
                    logger.info("[Loggable] Result - {}: {}", description, JSON.toJSONString(result));
                } catch (Exception e) {
                    logger.warn("[Loggable] Failed to serialize result: {}", e.getMessage());
                }
            }
            
            return result;
        } catch (Throwable throwable) {
            // 记录异常信息
            if (loggable != null) {
                logger.error("[Loggable] Error - {} - Class: {}, Method: {}, UserId: {}, Exception: {}", 
                    description, className, methodName, userId, throwable.getMessage(), throwable);
            } else {
                logger.error("Request error - Class: {}, Method: {}, UserId: {}, Exception: {}", 
                    className, methodName, userId, throwable.getMessage(), throwable);
            }
            throw throwable;
        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;
            
            // 记录请求结束
            if (loggable != null) {
                logger.info("[Loggable] End - {} - Class: {}, Method: {}, UserId: {}, Cost: {}ms", 
                    description, className, methodName, userId, costTime);
            } else {
                logger.info("Request end - Class: {}, Method: {}, UserId: {}, Cost: {}ms", 
                    className, methodName, userId, costTime);
            }
        }
    }
}