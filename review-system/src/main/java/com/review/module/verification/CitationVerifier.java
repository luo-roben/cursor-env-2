package com.review.module.verification;

import com.review.module.agent.dto.ReviewIssue;

import java.util.List;

public interface CitationVerifier {

    List<ReviewIssue> verify(List<ReviewIssue> issues);
}
