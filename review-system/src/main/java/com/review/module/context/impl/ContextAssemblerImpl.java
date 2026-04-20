package com.review.module.context.impl;

import com.review.infrastructure.vector.VectorStoreService;
import com.review.infrastructure.vector.dto.VectorSearchResult;
import com.review.module.agent.dto.*;
import com.review.module.cases.entity.ReviewCaseDO;
import com.review.module.cases.repository.ReviewCaseRepository;
import com.review.module.context.ContextAssembler;
import com.review.module.context.dto.AssembledContext;
import com.review.module.knowledge.entity.ContractTemplateClauseDO;
import com.review.module.knowledge.entity.ContractTemplateDO;
import com.review.module.knowledge.entity.LawArticleDO;
import com.review.module.knowledge.repository.ContractTemplateClauseRepository;
import com.review.module.knowledge.repository.ContractTemplateRepository;
import com.review.module.knowledge.repository.LawArticleRepository;
import com.review.module.rules.entity.TenantCustomRuleDO;
import com.review.module.rules.repository.TenantCustomRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAssemblerImpl implements ContextAssembler {

    private static final int MAX_ARTICLES = 20;
    private static final int MAX_CASES = 10;
    private static final int TOKEN_BUDGET = 4000;

    private final LawArticleRepository lawArticleRepository;
    private final TenantCustomRuleRepository tenantCustomRuleRepository;
    private final ReviewCaseRepository reviewCaseRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateClauseRepository contractTemplateClauseRepository;

    @Autowired(required = false)
    private VectorStoreService vectorStoreService;

    @Override
    public AssembledContext assemble(ReviewContext context) {
        List<LawArticleInfo> allArticles = new ArrayList<>();
        List<CaseInfo> allCases = new ArrayList<>();
        List<CustomRuleInfo> customRules = new ArrayList<>();
        List<TemplateDiffResult> templateDiffs = context.getTemplateDiffResults() != null ? context.getTemplateDiffResults() : new ArrayList<>();

        // Path A: by content type — query DB for published articles matching contentType
        if (context.getContentType() != null && !context.getContentType().isEmpty()) {
            try {
                List<LawArticleDO> contentTypeArticles = lawArticleRepository.findPublishedByContentType(context.getContentType());
                allArticles.addAll(toLawArticleInfoList(contentTypeArticles));
            } catch (Exception e) {
                log.warn("Failed to query articles by contentType: {}", e.getMessage());
            }
        }
        if (context.getLawArticles() != null) {
            allArticles.addAll(context.getLawArticles());
        }

        // Path B: by product type — query DB for published articles matching productType
        if (context.getProductType() != null && !context.getProductType().isEmpty()) {
            try {
                List<LawArticleDO> productTypeArticles = lawArticleRepository.findPublishedByProductType(context.getProductType());
                allArticles.addAll(toLawArticleInfoList(productTypeArticles));
            } catch (Exception e) {
                log.warn("Failed to query articles by productType: {}", e.getMessage());
            }
        }

        // Path C: by content semantics (vector search for similar law articles)
        allArticles.addAll(vectorSearchArticles(context.getContent()));

        // Path D: by similar cases (vector search)
        allCases.addAll(vectorSearchCases(context.getContent()));

        // Path E: custom rules by tenantId
        if (context.getTenantId() != null) {
            try {
                List<TenantCustomRuleDO> tenantRules = tenantCustomRuleRepository.findByTenantIdAndEnabledTrue(context.getTenantId());
                customRules.addAll(tenantRules.stream()
                        .map(r -> CustomRuleInfo.builder()
                                .id(r.getId())
                                .ruleType(r.getRuleType())
                                .content(r.getContent())
                                .matchMode(r.getMatchMode())
                                .severity(r.getSeverityIfTriggered())
                                .build())
                        .toList());
            } catch (Exception e) {
                log.warn("Failed to query tenant custom rules: {}", e.getMessage());
            }
        }
        if (context.getCustomRules() != null) {
            customRules.addAll(context.getCustomRules());
        }

        // Path F: contract template clause comparison
        if ("CONTRACT".equalsIgnoreCase(context.getDocumentType()) && context.getContractType() != null) {
            try {
                List<ContractTemplateDO> templates = contractTemplateRepository.findByContractTypeAndStatus(context.getContractType(), "published");
                if (!templates.isEmpty()) {
                    ContractTemplateDO template = templates.get(0);
                    List<ContractTemplateClauseDO> requiredClauses = contractTemplateClauseRepository
                            .findByTemplateIdAndIsRequired(template.getTemplateId(), 1);
                    for (ContractTemplateClauseDO clause : requiredClauses) {
                        if (context.getContent() != null && !context.getContent().contains(clause.getClauseTitle() != null ? clause.getClauseTitle() : clause.getClauseText())) {
                            templateDiffs.add(TemplateDiffResult.builder()
                                    .clauseNumber(clause.getClauseId())
                                    .templateClauseText(clause.getClauseText())
                                    .actualClauseText(null)
                                    .diffType("MISSING")
                                    .severity(clause.getRiskLevel() != null ? clause.getRiskLevel().toUpperCase() : "MAJOR")
                                    .build());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to query contract template clauses: {}", e.getMessage());
            }
        }

        // Also load cases from context
        if (context.getCases() != null) {
            allCases.addAll(context.getCases());
        }

        // Dedup articles by ID
        List<LawArticleInfo> dedupedArticles = deduplicateArticles(allArticles);

        // Apply token budget
        dedupedArticles = applyTokenBudget(dedupedArticles);

        // Dedup cases by ID
        List<CaseInfo> dedupedCases = deduplicateCases(allCases);

        String assembledPrompt = buildPrompt(context, dedupedArticles, customRules, dedupedCases);

        return AssembledContext.builder()
                .segments(context.getSegments())
                .relevantArticles(dedupedArticles)
                .customRules(customRules)
                .cases(dedupedCases)
                .templateDiffResults(templateDiffs)
                .assembledPrompt(assembledPrompt)
                .build();
    }

    private List<LawArticleInfo> toLawArticleInfoList(List<LawArticleDO> articles) {
        return articles.stream()
                .map(a -> LawArticleInfo.builder()
                        .id(a.getId())
                        .lawName(a.getLawName())
                        .articleId(a.getArticleId())
                        .originalText(a.getOriginalText())
                        .normType(a.getNormType())
                        .build())
                .toList();
    }

    private List<LawArticleInfo> vectorSearchArticles(String content) {
        if (vectorStoreService == null || content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<VectorSearchResult> results = vectorStoreService.search(content, "law_knowledge_vectors", 5);
            return results.stream()
                    .map(r -> LawArticleInfo.builder()
                            .id(r.getMetadata() != null && r.getMetadata().get("id") != null
                                    ? Long.parseLong(r.getMetadata().get("id").toString()) : null)
                            .lawName(getMetaString(r, "lawName"))
                            .articleId(getMetaString(r, "articleId"))
                            .originalText(getMetaString(r, "originalText"))
                            .normType(getMetaString(r, "normType"))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Vector search for articles failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<CaseInfo> vectorSearchCases(String content) {
        if (vectorStoreService == null || content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<VectorSearchResult> results = vectorStoreService.search(content, "review_cases_vectors", 5);
            return results.stream()
                    .map(r -> CaseInfo.builder()
                            .id(r.getMetadata() != null && r.getMetadata().get("id") != null
                                    ? Long.parseLong(r.getMetadata().get("id").toString()) : null)
                            .reviewedContent(getMetaString(r, "reviewedContent"))
                            .verdict(getMetaString(r, "verdict"))
                            .severity(getMetaString(r, "severity"))
                            .reason(getMetaString(r, "reason"))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Vector search for cases failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getMetaString(VectorSearchResult r, String key) {
        if (r.getMetadata() == null || r.getMetadata().get(key) == null) return null;
        return r.getMetadata().get(key).toString();
    }

    private List<LawArticleInfo> deduplicateArticles(List<LawArticleInfo> articles) {
        Map<String, LawArticleInfo> seen = new LinkedHashMap<>();
        for (LawArticleInfo article : articles) {
            String key = article.getId() != null ? article.getId().toString()
                    : (article.getLawName() + ":" + article.getArticleId());
            seen.putIfAbsent(key, article);
        }
        return new ArrayList<>(seen.values()).stream()
                .limit(MAX_ARTICLES)
                .toList();
    }

    private List<CaseInfo> deduplicateCases(List<CaseInfo> cases) {
        Map<String, CaseInfo> seen = new LinkedHashMap<>();
        for (CaseInfo caseInfo : cases) {
            String key = caseInfo.getId() != null ? caseInfo.getId().toString()
                    : (caseInfo.getReviewedContent() != null ? caseInfo.getReviewedContent().hashCode() + "" : UUID.randomUUID().toString());
            seen.putIfAbsent(key, caseInfo);
        }
        return new ArrayList<>(seen.values()).stream()
                .limit(MAX_CASES)
                .toList();
    }

    private List<LawArticleInfo> applyTokenBudget(List<LawArticleInfo> articles) {
        int totalTokens = 0;
        List<LawArticleInfo> budgeted = new ArrayList<>();
        for (LawArticleInfo article : articles) {
            int tokenEst = estimateTokens(article.getOriginalText());
            if (totalTokens + tokenEst <= TOKEN_BUDGET) {
                budgeted.add(article);
                totalTokens += tokenEst;
            } else {
                break;
            }
        }
        return budgeted;
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return text.length() / 2;
    }

    private String buildPrompt(ReviewContext context, List<LawArticleInfo> articles,
                               List<CustomRuleInfo> rules, List<CaseInfo> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append("Document Type: ").append(context.getDocumentType()).append("\n");
        sb.append("Content Type: ").append(context.getContentType()).append("\n\n");

        if (!articles.isEmpty()) {
            sb.append("## Relevant Law Articles\n");
            for (LawArticleInfo a : articles) {
                sb.append("- ").append(a.getLawName()).append(" ").append(a.getArticleId())
                        .append(": ").append(a.getOriginalText()).append("\n");
            }
        }

        if (!rules.isEmpty()) {
            sb.append("\n## Custom Rules\n");
            for (CustomRuleInfo r : rules) {
                sb.append("- [").append(r.getRuleType()).append("] ").append(r.getContent()).append("\n");
            }
        }

        if (!cases.isEmpty()) {
            sb.append("\n## Reference Cases\n");
            for (CaseInfo c : cases) {
                sb.append("- ").append(c.getVerdict()).append(": ").append(c.getReviewedContent()).append("\n");
            }
        }

        return sb.toString();
    }
}
