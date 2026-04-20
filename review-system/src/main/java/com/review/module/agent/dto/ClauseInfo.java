package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClauseInfo {

    private String clauseNumber;
    private String clauseTitle;
    private String clauseText;
    private String clauseType;
    private int charOffset;
    private int charLength;
}
