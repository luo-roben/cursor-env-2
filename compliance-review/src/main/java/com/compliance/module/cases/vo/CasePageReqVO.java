package com.compliance.module.cases.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CasePageReqVO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    private String verdict;

    private String contentType;

    private String productType;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
