package com.collab.common.utils;

/**
 * 当前登录用户上下文
 */
public class LoginUserContext {

    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置用户id
     */
    public static void setUserId(Long userId){
        THREAD_LOCAL.set(userId);
    }

    /**
     * 获取用户id
     */
    public static Long getUserId(){
        return THREAD_LOCAL.get();
    }

    /**
     * 清理用户信息
     */
    public static void clear(){
        THREAD_LOCAL.remove();
    }
}
