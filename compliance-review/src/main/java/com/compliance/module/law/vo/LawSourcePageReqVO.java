package com.compliance.module.law.vo;

import lombok.Data;

@Data
public class LawSourcePageReqVO {

    private String title;
    private String docType;
    private String status;
    private String parseStatus;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
