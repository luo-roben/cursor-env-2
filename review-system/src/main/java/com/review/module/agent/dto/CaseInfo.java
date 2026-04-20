package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInfo {

    private Long id;
    private String reviewedContent;
    private String verdict;
    private String severity;
    private String reason;
}
