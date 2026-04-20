package com.review.module.cases.vo;

import lombok.Data;

@Data
public class CasePageReqVO {

    private Long tenantId;
    private String documentType;
    private String contentType;
    private String contractType;
    private String verdict;
    private String status;
    private int pageNum = 1;
    private int pageSize = 20;
}
