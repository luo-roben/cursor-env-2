package com.review.common.exception;

import com.review.common.result.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public CommonResult<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("ServiceException: code={}, message={}, uri={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return CommonResult.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public CommonResult<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Bind failed: {}", message);
        return CommonResult.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), e.getMessage(), e);
        return CommonResult.error(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }
}
