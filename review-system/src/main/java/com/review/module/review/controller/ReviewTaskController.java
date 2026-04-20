package com.review.module.review.controller;

import com.review.common.result.CommonResult;
import com.review.common.result.PageResult;
import com.review.module.review.service.ReviewTaskService;
import com.review.module.review.vo.ReviewSubmitReqVO;
import com.review.module.review.vo.ReviewTaskRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审查任务管理")
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;

    @Operation(summary = "提交审查任务")
    @PostMapping("/submit")
    public CommonResult<ReviewTaskRespVO> submit(@Valid @RequestBody ReviewSubmitReqVO reqVO) {
        return CommonResult.success(reviewTaskService.submit(reqVO));
    }

    @Operation(summary = "根据ID获取审查任务详情")
    @GetMapping("/{id}")
    public CommonResult<ReviewTaskRespVO> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long tenantId) {
        return CommonResult.success(reviewTaskService.getById(id, tenantId));
    }

    @Operation(summary = "分页查询审查任务")
    @GetMapping
    public CommonResult<PageResult<ReviewTaskRespVO>> page(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(reviewTaskService.page(tenantId, reviewStatus, documentType, contentType, pageNum, pageSize));
    }
}
