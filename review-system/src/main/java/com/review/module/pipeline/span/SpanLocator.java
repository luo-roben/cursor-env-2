package com.review.module.pipeline.span;

import com.review.module.agent.dto.ReviewIssue;

public interface SpanLocator {
    ReviewIssue locate(ReviewIssue issue, String fullContent);
}
