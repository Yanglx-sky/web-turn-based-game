package cn.iocoder.gameweb.exception;

import cn.iocoder.gamecommon.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准格式的响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有异常
     * @param e 异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 记录异常日志
        logger.error("Global exception: {}", e.getMessage(), e);
        
        // 返回统一格式的错误响应
        return Result.error("服务器内部错误: " + e.getMessage());
    }

    /**
     * 处理运行时异常
     * @param e 运行时异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 记录异常日志
        logger.error("Runtime exception: {}", e.getMessage(), e);
        
        // 返回统一格式的错误响应
        return Result.error("运行时错误: " + e.getMessage());
    }

    /**
     * 处理空指针异常
     * @param e 空指针异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        // 记录异常日志
        logger.error("Null pointer exception: {}", e.getMessage(), e);
        
        // 返回统一格式的错误响应
        return Result.error("空指针错误: " + e.getMessage());
    }

    /**
     * 处理参数异常
     * @param e 参数异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        // 记录异常日志
        logger.error("Illegal argument exception: {}", e.getMessage(), e);
        
        // 返回统一格式的错误响应
        return Result.error("参数错误: " + e.getMessage());
    }

    /**
     * 处理静态资源未找到异常
     * 对于favicon.ico静默处理，其他资源记录警告日志
     * @param e 资源未找到异常
     * @return null（不返回内容，浏览器会使用默认图标）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e) {
        String resourcePath = e.getResourcePath();
        // 对favicon.ico静默处理，不记录日志
        if (resourcePath != null && resourcePath.contains("favicon.ico")) {
            return null;
        }
        // 其他静态资源缺失记录警告
        logger.warn("Static resource not found: {}", resourcePath);
        return Result.error("资源未找到: " + resourcePath);
    }
}