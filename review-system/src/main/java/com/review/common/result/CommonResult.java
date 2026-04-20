package com.review.common.result;

import com.review.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResult<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> CommonResult<T> success() {
        return new CommonResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> CommonResult<T> success(String message, T data) {
        return new CommonResult<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> CommonResult<T> error(int code, String message) {
        return new CommonResult<>(code, message, null);
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return new CommonResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode, String message) {
        return new CommonResult<>(errorCode.getCode(), message, null);
    }

    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
