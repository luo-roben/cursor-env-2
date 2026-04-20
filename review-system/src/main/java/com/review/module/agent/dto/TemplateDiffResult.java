package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDiffResult {

    private String clauseNumber;
    private String templateClauseText;
    private String actualClauseText;
    private String diffType;
    private String severity;
}
