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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
public class LogAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Pointcut("execution(* *..controller.*.*(..))")
    public void controllerPointcut() {
    }

    @Pointcut("@annotation(cn.iocoder.gamecommon.annotation.Loggable) || @within(cn.iocoder.gamecommon.annotation.Loggable)")
    public void loggablePointcut() {
    }

    @Around("controllerPointcut() || loggablePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Loggable loggable = method.getAnnotation(Loggable.class);
        if (loggable == null) {
            loggable = joinPoint.getTarget().getClass().getAnnotation(Loggable.class);
        }
        
        String description = loggable != null && !loggable.value().isEmpty() ? loggable.value() : methodName;
        
        String requestUri = "";
        String httpMethod = "";
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                requestUri = request.getRequestURI();
                httpMethod = request.getMethod();
            }
        } catch (Exception e) {
            logger.debug("Failed to get request context: {}", e.getMessage());
        }
        
        if (loggable != null) {
            logger.info("[{}] Start - {} {}", description, httpMethod, requestUri);
            
            if (loggable.recordParams()) {
                try {
                    Object[] args = joinPoint.getArgs();
                    Object[] serializableArgs = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            String paramClassName = args[i].getClass().getName();
                            if (paramClassName.startsWith("jakarta.servlet.http.") || paramClassName.startsWith("javax.servlet.http.")) {
                                serializableArgs[i] = paramClassName.substring(paramClassName.lastIndexOf(".") + 1) + " [filtered]";
                            } else {
                                try {
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
                    logger.info("[{}] Params: {}", description, JSON.toJSONString(serializableArgs));
                } catch (Exception e) {
                    logger.warn("[{}] Failed to serialize params: {}", description, e.getMessage());
                }
            }
        } else {
            logger.info("Request - {} {} | {}.{}", httpMethod, requestUri, className, methodName);
        }
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        
        try {
            result = joinPoint.proceed();
            
            if (loggable != null && loggable.recordResult()) {
                try {
                    String resultStr = JSON.toJSONString(result);
                    if (resultStr.length() > 1000) {
                        resultStr = resultStr.substring(0, 1000) + "... [truncated]";
                    }
                    logger.info("[{}] Result: {}", description, resultStr);
                } catch (Exception e) {
                    logger.warn("[{}] Failed to serialize result: {}", description, e.getMessage());
                }
            }
            
            return result;
        } catch (Throwable throwable) {
            logger.error("[{}] Error - {} {} | Exception: {}", 
                description != null ? description : methodName, httpMethod, requestUri, throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            
            if (loggable != null) {
                logger.info("[{}] End - {} {} | Cost: {}ms", description, httpMethod, requestUri, costTime);
            } else {
                logger.info("Request - {} {} | {}.{} | Cost: {}ms", 
                    httpMethod, requestUri, className, methodName, costTime);
            }
        }
    }
}