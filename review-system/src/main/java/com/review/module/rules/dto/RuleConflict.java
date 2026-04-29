package com.review.module.rules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleConflict {

    private Long ruleId1;
    private Long ruleId2;
    private String conflictType;
    private String description;
}
