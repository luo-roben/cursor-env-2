package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {

    private Integer cardCategory;
    private Integer segmentIndex;
    private String originalText;
    private String locationText;
    private Integer charOffset;
    private Integer charLength;
    private String verdict;
    private Double confidence;
    private String issueType;
    private String severity;
    private String description;
    private String citedArticleCode;
    private String citedLawName;
    private String suggestion;
    private String suggestionType;
    private String citationStatus;
}
