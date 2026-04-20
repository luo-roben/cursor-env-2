package com.review.module.feedback.controller;

import com.review.common.result.CommonResult;
import com.review.module.feedback.service.HumanFeedbackService;
import com.review.module.feedback.vo.FeedbackRespVO;
import com.review.module.feedback.vo.FeedbackSubmitReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "人工反馈", description = "人工反馈管理接口")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class HumanFeedbackController {

    private final HumanFeedbackService humanFeedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping
    public CommonResult<FeedbackRespVO> submit(@Valid @RequestBody FeedbackSubmitReqVO reqVO) {
        return CommonResult.success(humanFeedbackService.submit(reqVO));
    }

    @Operation(summary = "查询任务反馈列表")
    @GetMapping("/task/{taskId}")
    public CommonResult<List<FeedbackRespVO>> listByTaskId(@PathVariable Long taskId) {
        return CommonResult.success(humanFeedbackService.listByTaskId(taskId));
    }

    @Operation(summary = "查询反馈详情")
    @GetMapping("/{id}")
    public CommonResult<FeedbackRespVO> getById(@PathVariable Long id) {
        return CommonResult.success(humanFeedbackService.getById(id));
    }
}
