package com.review.module.review.controller;

import com.review.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping
    public CommonResult<Map<String, Object>> health() {
        return CommonResult.success(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "review-system"
        ));
    }
}
