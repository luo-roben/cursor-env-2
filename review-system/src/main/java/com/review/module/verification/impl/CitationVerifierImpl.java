package com.review.module.verification.impl;

import com.review.module.agent.dto.ReviewIssue;
import com.review.module.verification.CitationVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CitationVerifierImpl implements CitationVerifier {

    @Override
    public List<ReviewIssue> verify(List<ReviewIssue> issues) {
        List<ReviewIssue> verified = new ArrayList<>();

        for (ReviewIssue issue : issues) {
            ReviewIssue verifiedIssue = verifyIssue(issue);
            verified.add(verifiedIssue);
        }

        return verified;
    }

    private ReviewIssue verifyIssue(ReviewIssue issue) {
        if (issue.getCitedArticleCode() == null || issue.getCitedLawName() == null) {
            issue.setCitationStatus("UNVERIFIED");
            return issue;
        }

        // Step 1: Exact match
        boolean exactMatch = tryExactMatch(issue.getCitedLawName(), issue.getCitedArticleCode());
        if (exactMatch) {
            issue.setCitationStatus("VERIFIED");
            return issue;
        }

        // Step 2: Fuzzy match
        String correctedArticle = tryFuzzyMatch(issue.getCitedLawName(), issue.getCitedArticleCode());
        if (correctedArticle != null) {
            issue.setCitationStatus("CORRECTED");
            issue.setCitedArticleCode(correctedArticle);
            return issue;
        }

        // Step 3: Mark as hallucination
        issue.setCitationStatus("UNVERIFIED");
        return issue;
    }

    private boolean tryExactMatch(String lawName, String articleCode) {
        // In production, this would query LawArticleRepository
        // For MVP, we accept all citations from known law names
        List<String> knownLaws = List.of(
                "证券期货投资者适当性管理办法",
                "中华人民共和国民法典",
                "中华人民共和国合同法",
                "中华人民共和国证券法",
                "中华人民共和国广告法",
                "证券投资基金销售管理办法"
        );

        return knownLaws.stream().anyMatch(lawName::contains);
    }

    private String tryFuzzyMatch(String lawName, String articleCode) {
        // Fuzzy matching: try to find close matches
        // For MVP, return null (no correction)
        return null;
    }
}
