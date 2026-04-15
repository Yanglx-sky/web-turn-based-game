package cn.iocoder.gamecommon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口日志注解
 * 用于标记需要记录详细日志的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    /**
     * 日志描述
     */
    String value() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回值
     */
    boolean recordResult() default true;
}