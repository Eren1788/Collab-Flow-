package com.collab.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回数据给前端
 *
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    //成功并返回数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    //成功但不返回数据
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    //失败
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
