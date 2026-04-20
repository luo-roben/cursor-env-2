package com.review.module.pipeline.impl;

import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.*;
import com.review.module.context.ContextAssembler;
import com.review.module.context.dto.AssembledContext;
import com.review.module.filter.QuickFilterService;
import com.review.module.filter.dto.QuickFilterResult;
import com.review.module.parser.ContractClauseParser;
import com.review.module.parser.dto.ParsedClause;
import com.review.module.parser.dto.ParsedContract;
import com.review.module.pipeline.ReviewPipeline;
import com.review.module.pipeline.dto.ReviewPipelineResult;
import com.review.module.verification.CitationVerifier;
import com.review.module.verification.RiskScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewPipelineImpl implements ReviewPipeline {

    private final List<ReviewCardAgent> cardAgents;
    private final ContractClauseParser contractClauseParser;
    private final QuickFilterService quickFilterService;
    private final ContextAssembler contextAssembler;
    private final CitationVerifier citationVerifier;
    private final RiskScoreCalculator riskScoreCalculator;

    private final ExecutorService agentExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    @Override
    public ReviewPipelineResult execute(ReviewContext context) {
        long startTime = System.currentTimeMillis();
        log.info("Pipeline started for taskId={}", context.getTaskId());

        // Step 1: Classification (document type already provided in context)
        String documentType = context.getDocumentType() != null ? context.getDocumentType() : "OTHER";
        log.debug("Document type: {}", documentType);

        // Step 2: Content parsing
        List<String> segments = parseContent(context);
        context.setSegments(segments);

        if ("CONTRACT".equalsIgnoreCase(documentType) && context.getContent() != null) {
            ParsedContract parsedContract = contractClauseParser.parse(context.getContent());
            List<ClauseInfo> clauses = parsedContract.getClauses().stream()
                    .map(this::toClauseInfo)
                    .collect(Collectors.toList());
            context.setClauses(clauses);
        }

        // Step 3: Quick filter
        QuickFilterResult filterResult = quickFilterService.filter(
                context.getContent(), context.getCustomRules());
        List<String> missingElements = new ArrayList<>(filterResult.getMissingStatements());

        // Step 4: ACE context assembly
        AssembledContext assembledContext = contextAssembler.assemble(context);
        context.setLawArticles(assembledContext.getRelevantArticles());
        if (context.getCustomRules() == null || context.getCustomRules().isEmpty()) {
            context.setCustomRules(assembledContext.getCustomRules());
        }
        if (context.getCases() == null || context.getCases().isEmpty()) {
            context.setCases(assembledContext.getCases());
        }
        context.setTemplateDiffResults(assembledContext.getTemplateDiffResults());

        // Step 5: Run applicable agents in parallel
        List<ReviewCardAgent> applicableAgents = cardAgents.stream()
                .filter(agent -> agent.isApplicable(context))
                .toList();

        Map<Integer, List<ReviewIssue>> cardResults = new LinkedHashMap<>();
        List<ReviewIssue> allIssues = new ArrayList<>();

        List<CompletableFuture<AgentResult>> futures = applicableAgents.stream()
                .map(agent -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<ReviewIssue> issues = agent.review(context);
                        return new AgentResult(agent.getCardCategory(), issues);
                    } catch (Exception e) {
                        log.error("Agent {} failed: {}", agent.getCardCategory(), e.getMessage(), e);
                        return new AgentResult(agent.getCardCategory(), Collections.emptyList());
                    }
                }, agentExecutor))
                .toList();

        for (CompletableFuture<AgentResult> future : futures) {
            try {
                AgentResult result = future.join();
                cardResults.put(result.category.getCode(), result.issues);
                allIssues.addAll(result.issues);
            } catch (Exception e) {
                log.error("Error collecting agent result", e);
            }
        }

        // Add quick filter banned word hits as issues
        for (QuickFilterResult.BannedWordHit hit : filterResult.getBannedWordHits()) {
            allIssues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.REDLINES.getCode())
                    .charOffset(hit.getOffset())
                    .charLength(hit.getWord().length())
                    .originalText(hit.getWord())
                    .verdict("VIOLATION")
                    .confidence(1.0)
                    .issueType("quick_filter_banned_word")
                    .severity(hit.getSeverity())
                    .description("快速过滤命中禁用词: " + hit.getWord())
                    .suggestion("删除或替换禁用词'" + hit.getWord() + "'")
                    .suggestionType("REVISION")
                    .build());
        }

        // Step 6: Post-processing
        List<ReviewIssue> verifiedIssues = citationVerifier.verify(allIssues);

        RiskScoreCalculator.RiskScoreResult riskResult = riskScoreCalculator.calculate(
                verifiedIssues, missingElements);

        // Step 7: Determine overall verdict
        String overallVerdict = determineOverallVerdict(verifiedIssues);

        long totalLatency = System.currentTimeMillis() - startTime;
        log.info("Pipeline completed for taskId={}, issues={}, riskScore={}, latencyMs={}",
                context.getTaskId(), verifiedIssues.size(), riskResult.score(), totalLatency);

        return ReviewPipelineResult.builder()
                .results(verifiedIssues)
                .missingElements(missingElements)
                .cardResults(cardResults)
                .overallVerdict(overallVerdict)
                .riskScore(riskResult.score())
                .riskLevel(riskResult.level())
                .totalLatencyMs(totalLatency)
                .build();
    }

    private List<String> parseContent(ReviewContext context) {
        if (context.getContent() == null) return Collections.emptyList();
        String[] paragraphs = context.getContent().split("\n\n+");
        List<String> segments = new ArrayList<>();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        return segments;
    }

    private ClauseInfo toClauseInfo(ParsedClause parsed) {
        return ClauseInfo.builder()
                .clauseNumber(parsed.getClauseNumber())
                .clauseTitle(parsed.getClauseTitle())
                .clauseText(parsed.getClauseText())
                .clauseType(parsed.getClauseType())
                .charOffset(parsed.getCharOffset())
                .charLength(parsed.getCharLength())
                .build();
    }

    private String determineOverallVerdict(List<ReviewIssue> issues) {
        boolean hasViolation = issues.stream()
                .anyMatch(i -> "VIOLATION".equalsIgnoreCase(i.getVerdict()));
        boolean hasNeedsReview = issues.stream()
                .anyMatch(i -> "NEEDS_REVIEW".equalsIgnoreCase(i.getVerdict()));

        if (hasViolation) return "VIOLATION";
        if (hasNeedsReview) return "NEEDS_REVIEW";
        return "COMPLIANT";
    }

    private record AgentResult(CardCategory category, List<ReviewIssue> issues) {}
}
