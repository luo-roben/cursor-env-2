package com.review.module.agent;

import com.review.common.enums.CardCategory;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;

import java.util.List;

public interface ReviewCardAgent {

    CardCategory getCardCategory();

    List<ReviewIssue> review(ReviewContext context);

    boolean isApplicable(ReviewContext context);
}
