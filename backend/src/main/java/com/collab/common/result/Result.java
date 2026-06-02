package com.collab.common.result;

import lombok.Data;

@Data
public class Result<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功
     */
    public static <T> Result<T> success(){

        Result<T> result = new Result<>();

        result.setCode(200);

        result.setMessage("success");

        return result;
    }

    /**
     * 成功带数据
     */
    public static <T> Result<T> success(T data){

        Result<T> result = new Result<>();

        result.setCode(200);

        result.setMessage("success");

        result.setData(data);

        return result;
    }

    /**
     * 失败
     */
    public static <T> Result<T> error(String message){

        Result<T> result = new Result<>();

        result.setCode(500);

        result.setMessage(message);

        return result;
    }

    /**
     * 自定义状态码
     */
    public static <T> Result<T> error(
            Integer code,
            String message
    ){

        Result<T> result = new Result<>();

        result.setCode(code);

        result.setMessage(message);

        return result;
    }

}