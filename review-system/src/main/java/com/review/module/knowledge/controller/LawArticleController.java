package com.review.module.knowledge.controller;

import com.review.common.result.CommonResult;
import com.review.common.result.PageResult;
import com.review.module.knowledge.service.LawArticleService;
import com.review.module.knowledge.vo.LawArticleCreateReqVO;
import com.review.module.knowledge.vo.LawArticlePageReqVO;
import com.review.module.knowledge.vo.LawArticleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "法条知识管理")
@RestController
@RequestMapping("/api/v1/law/articles")
@RequiredArgsConstructor
public class LawArticleController {

    private final LawArticleService lawArticleService;

    @Operation(summary = "创建法条")
    @PostMapping
    public CommonResult<LawArticleRespVO> create(@Valid @RequestBody LawArticleCreateReqVO reqVO) {
        return CommonResult.success(lawArticleService.create(reqVO));
    }

    @Operation(summary = "根据ID获取法条")
    @GetMapping("/{id}")
    public CommonResult<LawArticleRespVO> getById(@PathVariable Long id) {
        return CommonResult.success(lawArticleService.getById(id));
    }

    @Operation(summary = "分页查询法条")
    @GetMapping
    public CommonResult<PageResult<LawArticleRespVO>> page(LawArticlePageReqVO reqVO) {
        return CommonResult.success(lawArticleService.page(reqVO));
    }

    @Operation(summary = "发布法条")
    @PutMapping("/{id}/publish")
    public CommonResult<Void> publish(@PathVariable Long id) {
        lawArticleService.publish(id);
        return CommonResult.success();
    }

    @Operation(summary = "更新法条")
    @PutMapping("/{id}")
    public CommonResult<LawArticleRespVO> update(@PathVariable Long id, @Valid @RequestBody LawArticleCreateReqVO reqVO) {
        return CommonResult.success(lawArticleService.update(id, reqVO));
    }

    @Operation(summary = "废弃法条")
    @PutMapping("/{id}/deprecate")
    public CommonResult<Void> deprecate(@PathVariable Long id) {
        lawArticleService.deprecate(id);
        return CommonResult.success();
    }

    @Operation(summary = "删除法条")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        lawArticleService.delete(id);
        return CommonResult.success();
    }
}
