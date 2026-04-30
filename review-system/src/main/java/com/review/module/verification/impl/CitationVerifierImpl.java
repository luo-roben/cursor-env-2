package com.review.module.verification.impl;

import com.review.module.agent.dto.ReviewIssue;
import com.review.module.knowledge.entity.LawArticleDO;
import com.review.module.knowledge.entity.LawNameAliasDO;
import com.review.module.knowledge.repository.LawArticleRepository;
import com.review.module.knowledge.repository.LawNameAliasRepository;
import com.review.module.verification.CitationVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitationVerifierImpl implements CitationVerifier {

    private final LawArticleRepository lawArticleRepository;

    @Autowired(required = false)
    private LawNameAliasRepository lawNameAliasRepository;

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

        boolean exactMatch = tryExactMatch(issue.getCitedLawName(), issue.getCitedArticleCode());
        if (exactMatch) {
            issue.setCitationStatus("VERIFIED");
            return issue;
        }

        String correctedArticle = tryFuzzyMatch(issue.getCitedLawName(), issue.getCitedArticleCode());
        if (correctedArticle != null) {
            issue.setCitationStatus("CORRECTED");
            issue.setCitedArticleCode(correctedArticle);
            return issue;
        }

        issue.setCitationStatus("UNVERIFIED");
        return issue;
    }

    private boolean tryExactMatch(String lawName, String articleCode) {
        Optional<LawArticleDO> article = lawArticleRepository.findByArticleIdAndLawName(articleCode, lawName);
        if (article.isPresent()) {
            return true;
        }

        List<LawArticleDO> publishedArticles = lawArticleRepository.findByStatus("published");
        return publishedArticles.stream()
                .anyMatch(a -> a.getLawName().contains(lawName) || lawName.contains(a.getLawName())
                        && a.getArticleId().equals(articleCode));
    }

    private String tryFuzzyMatch(String lawName, String articleCode) {
        if (lawNameAliasRepository != null) {
            try {
                List<LawNameAliasDO> aliases = lawNameAliasRepository.findByAlias(lawName);
                for (LawNameAliasDO aliasDO : aliases) {
                    String canonicalName = aliasDO.getLawName();
                    Optional<LawArticleDO> aliasMatch = lawArticleRepository.findByArticleIdAndLawName(articleCode, canonicalName);
                    if (aliasMatch.isPresent()) {
                        log.info("Alias match found: {} ({}) -> {}", lawName, canonicalName, articleCode);
                        return aliasMatch.get().getArticleId();
                    }
                }
            } catch (Exception e) {
                log.warn("Alias lookup failed for {}: {}", lawName, e.getMessage());
            }
        }

        List<LawArticleDO> publishedArticles = lawArticleRepository.findByStatus("published");

        Optional<LawArticleDO> match = publishedArticles.stream()
                .filter(a -> (a.getLawName().contains(lawName) || lawName.contains(a.getLawName()))
                        && a.getArticleId().startsWith(articleCode.length() > 2 ? articleCode.substring(0, 2) : articleCode))
                .findFirst();

        if (match.isPresent()) {
            log.info("Fuzzy match found: {} -> {}", articleCode, match.get().getArticleId());
            return match.get().getArticleId();
        }

        return null;
    }
}
