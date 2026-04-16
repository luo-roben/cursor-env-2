package com.compliance.module.verification.service.impl;

import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.CitationVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitationVerifierImpl implements CitationVerifier {

    private final LawArticleRepository lawArticleRepository;

    @Override
    public void verifyCitations(List<ReviewResultDO> results) {
        for (ReviewResultDO result : results) {
            if (result.getCitedArticleCode() == null || result.getCitedLawName() == null) {
                if ("violation".equals(result.getVerdict())) {
                    result.setCitationStatus("unverified");
                }
                continue;
            }

            try {
                List<LawArticleDO> matches = lawArticleRepository.findByArticleIdAndStatus(
                        result.getCitedArticleCode(), "published");

                LawArticleDO matched = matches.stream()
                        .filter(a -> a.getLawName() != null && a.getLawName().contains(result.getCitedLawName()))
                        .findFirst()
                        .orElse(matches.isEmpty() ? null : matches.get(0));

                if (matched != null) {
                    result.setLawArticleId(matched.getId());
                    result.setVerifiedOriginalText(matched.getOriginalText());
                    if (matched.getLawName().equals(result.getCitedLawName())) {
                        result.setCitationStatus("verified");
                    } else {
                        result.setCitationStatus("corrected");
                    }
                    log.debug("Citation verified: {} - {}", result.getCitedLawName(), result.getCitedArticleCode());
                } else {
                    result.setCitationStatus("unverified");
                    log.warn("Citation NOT found: {} - {}", result.getCitedLawName(), result.getCitedArticleCode());
                }
            } catch (Exception e) {
                result.setCitationStatus("unverified");
                log.warn("Citation verification error for {} - {}: {}", 
                        result.getCitedLawName(), result.getCitedArticleCode(), e.getMessage());
            }
        }
    }
}
