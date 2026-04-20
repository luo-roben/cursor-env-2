package com.review.module.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossReference {

    private String sourceClause;
    private String targetClause;
    private String referenceText;
    private int charOffset;
}
