package com.compliance.module.feedback.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.module.feedback.service.HumanFeedbackService;
import com.compliance.module.feedback.vo.FeedbackRespVO;
import com.compliance.module.feedback.vo.FeedbackSubmitReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "人工复核", description = "Human feedback management for review tasks")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class HumanFeedbackController {

    private final HumanFeedbackService humanFeedbackService;

    @Operation(summary = "提交人工反馈", description = "Submit human review feedback for a review task")
    @PostMapping("/submit")
    public CommonResult<FeedbackRespVO> submit(@Valid @RequestBody FeedbackSubmitReqVO reqVO) {
        return CommonResult.success(humanFeedbackService.submit(reqVO));
    }

    @Operation(summary = "获取任务的人工反馈列表", description = "Get all human feedbacks for a review task")
    @GetMapping("/task/{taskId}")
    public CommonResult<List<FeedbackRespVO>> listByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        return CommonResult.success(humanFeedbackService.listByTaskId(taskId, tenantId));
    }
}
