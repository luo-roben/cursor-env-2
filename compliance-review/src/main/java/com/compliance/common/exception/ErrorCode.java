package com.compliance.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    LAW_ARTICLE_NOT_FOUND(1001, "法条不存在"),
    LAW_ARTICLE_ALREADY_PUBLISHED(1002, "法条已发布，不可重复发布"),
    LAW_SOURCE_NOT_FOUND(1003, "法规来源不存在"),

    REVIEW_TASK_NOT_FOUND(2001, "审查任务不存在"),
    REVIEW_CONTENT_EMPTY(2002, "审查内容不能为空"),

    TENANT_RULE_NOT_FOUND(3001, "自定义规则不存在"),

    AI_SERVICE_ERROR(4001, "AI服务调用失败"),
    AI_RESPONSE_PARSE_ERROR(4002, "AI响应解析失败"),

    FEEDBACK_TASK_NOT_FOUND(5001, "复核任务不存在"),

    CASE_NOT_FOUND(6001, "案例不存在");

    private final int code;
    private final String msg;
}
