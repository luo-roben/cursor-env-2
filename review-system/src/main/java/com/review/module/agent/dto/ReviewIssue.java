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

    private int cardCategory;
    private int segmentIndex;
    private String originalText;
    private String locationText;
    private int charOffset;
    private int charLength;
    private String verdict;
    private double confidence;
    private String issueType;
    private String severity;
    private String description;
    private String citedArticleCode;
    private String citedLawName;
    private String suggestion;
    private String suggestionType;
    private String citationStatus;
}
