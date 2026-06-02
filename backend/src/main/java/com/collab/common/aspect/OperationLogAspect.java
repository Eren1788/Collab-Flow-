package com.collab.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.collab.common.annotation.OperationLogAnnotation;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.OperationLog;
import com.collab.entity.User;
import com.collab.mapper.OperationLogMapper;
import com.collab.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 切面类
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    private final UserMapper userMapper;

    @Around("@annotation(com.collab.common.annotation.OperationLogAnnotation)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();

        // 获取request
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = attributes.getRequest();

        // 获取方法
        MethodSignature signature = (MethodSignature)
                joinPoint.getSignature();

        Method method = joinPoint
                .getTarget()
                .getClass()
                .getMethod(
                        signature.getName(),
                        signature.getParameterTypes()
                );

        // 获取注解
        OperationLogAnnotation annotation = method.getAnnotation(OperationLogAnnotation.class);

        // 操作名称
        String operation = annotation.value();

        // 当前用户
        Long userId = LoginUserContext.getUserId();

        String username = "";

        if (userId != null) {

            User user = userMapper.selectById(userId);

            if (user != null) {
                username = user.getUsername();
            }
        }

        // 构建日志
        OperationLog operationLog = new OperationLog();

        operationLog.setUserId(userId);

        operationLog.setUsername(username);

        operationLog.setOperation(operation);

        operationLog.setMethod(request.getMethod());

        operationLog.setRequestUri(request.getRequestURI());

        operationLog.setIp(request.getRemoteAddr());

        operationLog.setRequestParams(
                JSON.toJSONString(
                        joinPoint.getArgs()
                )
        );

        try {

            Object result = joinPoint.proceed();

            operationLog.setStatus(1);

            operationLogMapper.insert(operationLog);

            log.info(
                    "操作日志记录成功，耗时：{}ms",
                    System.currentTimeMillis() - start
            );

            return result;

        } catch (Exception e) {

            operationLog.setStatus(0);
            operationLog.setErrorMsg(
                    e.getMessage()
            );

            operationLogMapper.insert(operationLog);
            throw e;
        }
    }
}