package com.compliance.module.review.manager;

import com.compliance.module.ai.dto.AiReviewRequest;
import com.compliance.module.ai.dto.AiReviewResponse;
import com.compliance.module.ai.service.AiReviewService;
import com.compliance.module.context.dto.AssembledContext;
import com.compliance.module.context.service.ContextAssembler;
import com.compliance.module.filter.dto.MissingElementResult;
import com.compliance.module.filter.dto.QuickFilterResult;
import com.compliance.module.filter.service.CompletenessCheckerService;
import com.compliance.module.filter.service.QuickFilterService;
import com.compliance.module.llm.entity.LlmCallLogDO;
import com.compliance.module.llm.service.LlmCallLogService;
import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewTaskDO;
import com.compliance.module.review.repository.ReviewMissingElementRepository;
import com.compliance.module.review.repository.ReviewResultRepository;
import com.compliance.module.review.repository.ReviewTaskRepository;
import com.compliance.module.verification.service.CitationVerifier;
import com.compliance.module.verification.service.RiskScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full 6-stage compliance review pipeline:
 * Stage 1: Quick filter (banned words + required statements)
 * Stage 2: Content parsing (segments)
 * Stage 3: ACE context assembly (law articles + custom rules + prompt)
 * Stage 4: LLM call (mock for MVP)
 * Stage 5: Post-processing (citation verification + risk scoring)
 * Stage 6: Persist results + completeness check
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewManager {

    private final ContextAssembler contextAssembler;
    private final AiReviewService aiReviewService;
    private final CitationVerifier citationVerifier;
    private final RiskScoreCalculator riskScoreCalculator;
    private final QuickFilterService quickFilterService;
    private final CompletenessCheckerService completenessCheckerService;
    private final LlmCallLogService llmCallLogService;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReviewMissingElementRepository reviewMissingElementRepository;

    @Value("${compliance.llm.default-model:mock}")
    private String defaultModel;

    @Transactional
    public ReviewTaskDO executeReview(ReviewTaskDO task) {
        long startTime = System.currentTimeMillis();

        task.setReviewStatus("reviewing");
        task.setLlmModel(defaultModel);
        reviewTaskRepository.save(task);

        log.info("Starting review pipeline for task: id={}, contentType={}", task.getId(), task.getContentType());

        List<ReviewResultDO> allResults = new ArrayList<>();
        List<ReviewMissingElementDO> allMissing = new ArrayList<>();

        // ===== Stage 1: Quick Filter (banned words + required statements) =====
        QuickFilterResult filterResult = quickFilterService.filter(task.getOriginalContent(), task.getTenantId());
        if (!filterResult.getBannedWordHits().isEmpty()) {
            log.info("Quick filter: found {} banned word hits", filterResult.getBannedWordHits().size());
            int idx = 0;
            for (QuickFilterResult.BannedWordHit hit : filterResult.getBannedWordHits()) {
                ReviewResultDO result = ReviewResultDO.builder()
                        .taskId(task.getId())
                        .tenantId(task.getTenantId())
                        .segmentIndex(-(++idx))
                        .originalText(hit.getWord())
                        .verdict("violation")
                        .confidence(new BigDecimal("1.00"))
                        .issueType("banned_word")
                        .severity("critical")
                        .description("命中企业禁用词: \"" + hit.getWord() + "\"")
                        .citationStatus("verified")
                        .suggestion("请删除禁用词\"" + hit.getWord() + "\"")
                        .build();
                allResults.add(result);
            }
        }
        for (QuickFilterResult.MissingStatement ms : filterResult.getMissingStatements()) {
            ReviewMissingElementDO missing = ReviewMissingElementDO.builder()
                    .taskId(task.getId())
                    .tenantId(task.getTenantId())
                    .element(ms.getStatement())
                    .requirement("企业必备声明缺失")
                    .severity(ms.getSeverity() != null ? ms.getSeverity() : "major")
                    .suggestion("请添加必备声明: \"" + ms.getStatement() + "\"")
                    .build();
            allMissing.add(missing);
        }

        // ===== Stage 2 & 3: Context Assembly =====
        AssembledContext context = contextAssembler.assemble(
                task.getOriginalContent(),
                task.getContentType(),
                task.getProductType(),
                task.getTenantId());

        task.setLlmRawPrompt(context.getAssembledPrompt());

        // ===== Stage 4: LLM Call =====
        AiReviewRequest aiRequest = AiReviewRequest.builder()
                .content(task.getOriginalContent())
                .contentType(task.getContentType())
                .productType(task.getProductType())
                .channel(task.getChannel())
                .assembledPrompt(context.getAssembledPrompt())
                .build();

        long llmStart = System.currentTimeMillis();
        AiReviewResponse aiResponse = aiReviewService.review(aiRequest);
        long llmLatency = System.currentTimeMillis() - llmStart;

        task.setLlmRawResponse(aiResponse.getRawResponse());
        task.setLlmLatencyMs((int) aiResponse.getLatencyMs());

        logLlmCall(task.getId(), context.getAssembledPrompt(), aiResponse.getRawResponse(),
                (int) llmLatency, true, null);

        // Build result entities from LLM response
        if (aiResponse.getSegments() != null) {
            for (AiReviewResponse.SegmentResult seg : aiResponse.getSegments()) {
                ReviewResultDO result = ReviewResultDO.builder()
                        .taskId(task.getId())
                        .tenantId(task.getTenantId())
                        .segmentIndex(seg.getSegmentIndex())
                        .originalText(seg.getOriginalText())
                        .verdict(seg.getVerdict())
                        .confidence(seg.getConfidence() != null ? seg.getConfidence() : BigDecimal.ZERO)
                        .issueType(seg.getIssueType())
                        .severity(seg.getSeverity())
                        .description(seg.getDescription())
                        .citedArticleCode(seg.getCitedArticleCode())
                        .citedLawName(seg.getCitedLawName())
                        .suggestion(seg.getSuggestion())
                        .citationStatus("pending")
                        .build();
                allResults.add(result);
            }
        }

        if (aiResponse.getMissingElements() != null) {
            for (AiReviewResponse.MissingElement me : aiResponse.getMissingElements()) {
                ReviewMissingElementDO missing = ReviewMissingElementDO.builder()
                        .taskId(task.getId())
                        .tenantId(task.getTenantId())
                        .element(me.getElement())
                        .requirement(me.getRequirement())
                        .severity(me.getSeverity() != null ? me.getSeverity() : "major")
                        .suggestion(me.getSuggestion())
                        .build();
                allMissing.add(missing);
            }
        }

        // ===== Stage 5: Post-processing =====
        // Citation verification (anti-hallucination)
        citationVerifier.verifyCitations(allResults);

        // Completeness check
        List<MissingElementResult> completenessResults = completenessCheckerService.check(
                task.getOriginalContent(), task.getContentType(), task.getProductType(), task.getTenantId());
        for (MissingElementResult cr : completenessResults) {
            boolean alreadyReported = allMissing.stream()
                    .anyMatch(m -> m.getElement().contains(cr.getCheckItem()));
            if (!alreadyReported) {
                ReviewMissingElementDO missing = ReviewMissingElementDO.builder()
                        .taskId(task.getId())
                        .tenantId(task.getTenantId())
                        .element(cr.getCheckItem())
                        .requirement(cr.getRequirement())
                        .lawArticleId(cr.getLawArticleId())
                        .severity(cr.getSeverity() != null ? cr.getSeverity() : "major")
                        .suggestion(cr.getSuggestion())
                        .build();
                allMissing.add(missing);
            }
        }

        // ===== Stage 6: Persist & Finalize =====
        allResults = reviewResultRepository.saveAll(allResults);
        allMissing = reviewMissingElementRepository.saveAll(allMissing);

        int riskScore = riskScoreCalculator.calculateRiskScore(allResults, allMissing);
        String riskLevel = riskScoreCalculator.calculateRiskLevel(riskScore);

        task.setOverallVerdict(aiResponse.getOverallVerdict());
        task.setRiskScore(riskScore);
        task.setRiskLevel(riskLevel);
        task.setReviewStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        task.setTotalLatencyMs((int) (System.currentTimeMillis() - startTime));

        task = reviewTaskRepository.save(task);

        log.info("Review pipeline completed: taskId={}, verdict={}, riskScore={}, riskLevel={}, " +
                        "bannedWordHits={}, missingElements={}, totalLatency={}ms",
                task.getId(), task.getOverallVerdict(), riskScore, riskLevel,
                filterResult.getBannedWordHits().size(), allMissing.size(), task.getTotalLatencyMs());

        return task;
    }

    private void logLlmCall(Long taskId, String prompt, String response, int latencyMs,
                            boolean success, String errorMessage) {
        try {
            LlmCallLogDO logEntry = LlmCallLogDO.builder()
                    .reviewTaskId(taskId)
                    .callType("review")
                    .modelName(defaultModel)
                    .rawPrompt(prompt)
                    .rawResponse(response)
                    .latencyMs(latencyMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();
            llmCallLogService.log(logEntry);
        } catch (Exception e) {
            log.warn("Failed to log LLM call for task {}: {}", taskId, e.getMessage());
        }
    }
}
