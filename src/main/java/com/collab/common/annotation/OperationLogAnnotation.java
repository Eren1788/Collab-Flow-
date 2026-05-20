package com.collab.common.annotation;

import java.lang.annotation.*;

/**
 * 切面注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {

    /**
     * 操作名称
     */
    String value();

}