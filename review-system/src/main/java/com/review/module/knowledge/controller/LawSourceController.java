package com.review.module.knowledge.controller;

import com.review.common.result.CommonResult;
import com.review.common.result.PageResult;
import com.review.module.knowledge.service.LawSourcePipelineService;
import com.review.module.knowledge.service.LawSourceService;
import com.review.module.knowledge.vo.LawSourceCreateReqVO;
import com.review.module.knowledge.vo.LawSourcePageReqVO;
import com.review.module.knowledge.vo.LawSourceRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "法规来源管理")
@RestController
@RequestMapping("/api/v1/law/sources")
@RequiredArgsConstructor
public class LawSourceController {

    private final LawSourceService lawSourceService;
    private final LawSourcePipelineService lawSourcePipelineService;

    @Operation(summary = "创建法规来源")
    @PostMapping
    public CommonResult<LawSourceRespVO> create(@Valid @RequestBody LawSourceCreateReqVO reqVO) {
        return CommonResult.success(lawSourceService.create(reqVO));
    }

    @Operation(summary = "根据ID获取法规来源")
    @GetMapping("/{id}")
    public CommonResult<LawSourceRespVO> getById(@PathVariable Long id) {
        return CommonResult.success(lawSourceService.getById(id));
    }

    @Operation(summary = "分页查询法规来源")
    @GetMapping
    public CommonResult<PageResult<LawSourceRespVO>> page(LawSourcePageReqVO reqVO) {
        return CommonResult.success(lawSourceService.page(reqVO));
    }

    @Operation(summary = "更新解析状态")
    @PutMapping("/{id}/parse-status")
    public CommonResult<Void> updateParseStatus(@PathVariable Long id, @RequestParam String parseStatus) {
        lawSourceService.updateParseStatus(id, parseStatus);
        return CommonResult.success();
    }

    @Operation(summary = "触发法规解析")
    @PostMapping("/{id}/parse")
    public CommonResult<Void> triggerParsing(@PathVariable Long id) {
        lawSourcePipelineService.triggerParsing(id);
        return CommonResult.success();
    }

    @Operation(summary = "确认所有解析法条")
    @PostMapping("/{id}/confirm")
    public CommonResult<Void> confirmAll(@PathVariable Long id, @RequestParam(required = false) Long confirmedBy) {
        lawSourcePipelineService.confirmAll(id, confirmedBy);
        return CommonResult.success();
    }
}
