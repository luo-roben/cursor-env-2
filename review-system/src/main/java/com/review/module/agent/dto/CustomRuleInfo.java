package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRuleInfo {

    private Long id;
    private String ruleType;
    private String content;
    private String matchMode;
    private String severity;
}
