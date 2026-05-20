package com.collab.common.exception;

import com.collab.common.result.Result;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e){
        log.error("业务异常：{}",e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * JWT异常
     */
    @ExceptionHandler(JwtException.class)
    public Result<?> handleJwtException(JwtException e){
        log.error("JWT异常：{}",e.getMessage());
        return Result.error(401,"登录已过期");
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e){
        String message = e.getBindingResult()
                        .getFieldError()
                        .getDefaultMessage();
        log.error("参数异常：{}",message);
        return Result.error(message);
    }

    /**
     * RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e){
        log.error("运行时异常：",e);
        return Result.error(e.getMessage());
    }

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e){
        log.error("系统异常：",e);
        return Result.error("系统异常");
    }

}