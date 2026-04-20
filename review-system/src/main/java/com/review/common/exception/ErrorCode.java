package com.review.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // Law module: 1001-1099
    LAW_SOURCE_NOT_FOUND(1001, "法规来源不存在"),
    LAW_ARTICLE_NOT_FOUND(1002, "法条不存在"),
    LAW_ARTICLE_ALREADY_PUBLISHED(1003, "法条已发布，不可重复发布"),

    // Review module: 2001-2099
    REVIEW_TASK_NOT_FOUND(2001, "审查任务不存在"),
    REVIEW_CONTENT_EMPTY(2002, "审查内容不能为空"),

    // Custom rule module: 3001-3099
    CUSTOM_RULE_NOT_FOUND(3001, "自定义规则不存在"),

    // AI module: 4001-4099
    AI_SERVICE_ERROR(4001, "AI服务调用失败"),
    AI_RESPONSE_PARSE_ERROR(4002, "AI响应解析失败"),

    // Feedback module: 5001-5099
    FEEDBACK_NOT_FOUND(5001, "反馈记录不存在"),

    // Case module: 6001-6099
    CASE_NOT_FOUND(6001, "案例不存在"),

    // Template module: 7001-7099
    TEMPLATE_NOT_FOUND(7001, "合同模板不存在"),
    TEMPLATE_CLAUSE_NOT_FOUND(7002, "模板条款不存在"),

    // Checklist module: 8001-8099
    CHECKLIST_NOT_FOUND(8001, "自检清单不存在");

    private final int code;
    private final String message;

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
