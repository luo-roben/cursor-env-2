package com.compliance.module.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingElementResult {

    private String checkItem;
    private String requirement;
    private Long lawArticleId;
    private String severity;
    private String suggestion;
}
