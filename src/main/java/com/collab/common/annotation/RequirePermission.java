package com.collab.common.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限编码
     */
    String value();

}