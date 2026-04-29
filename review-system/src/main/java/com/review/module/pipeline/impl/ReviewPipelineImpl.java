package com.review.module.pipeline.impl;

import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.*;
import com.review.module.context.ContextAssembler;
import com.review.module.context.dto.AssembledContext;
import com.review.module.filter.QuickFilterService;
import com.review.module.filter.dto.QuickFilterResult;
import com.review.module.parser.ContentParserFactory;
import com.review.module.parser.ContractClauseParser;
import com.review.module.parser.dto.ParsedClause;
import com.review.module.parser.dto.ParsedContent;
import com.review.module.parser.dto.ParsedContract;
import com.review.module.pipeline.ReviewPipeline;
import com.review.module.pipeline.dto.ReviewPipelineResult;
import com.review.module.pipeline.span.SpanLocator;
import com.review.module.registry.DocumentTypeConfig;
import com.review.module.registry.DocumentTypeRegistry;
import com.review.module.review.entity.ReviewCardResultDO;
import com.review.module.review.service.ClauseGraphService;
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
    private final SpanLocator spanLocator;
    private final DocumentTypeRegistry documentTypeRegistry;
    private final ContentParserFactory contentParserFactory;
    private final ClauseGraphService clauseGraphService;

    private final ExecutorService agentExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    private static final int SMALL_DOC_THRESHOLD = 5000;
    private static final int MEDIUM_DOC_THRESHOLD = 30000;
    private static final int DEFAULT_CHUNK_SIZE = 2000;
    private static final int CHUNK_OVERLAP = 100;

    @Override
    public ReviewPipelineResult execute(ReviewContext context) {
        long startTime = System.currentTimeMillis();
        log.info("Pipeline started for taskId={}", context.getTaskId());

        String documentType = context.getDocumentType() != null ? context.getDocumentType() : "OTHER";
        log.debug("Document type: {}", documentType);

        DocumentTypeConfig docConfig = documentTypeRegistry.getConfig(documentType);
        Set<Integer> applicableCards = docConfig != null ? docConfig.getApplicableCards() : null;

        ParsedContent parsedContent = contentParserFactory.getParser(documentType).parse(context.getContent());
        context.setSegments(parsedContent.getSegments());

        if ("CONTRACT".equalsIgnoreCase(documentType) && context.getContent() != null) {
            if (parsedContent.getClauses() != null && !parsedContent.getClauses().isEmpty()) {
                List<ClauseInfo> clauses = parsedContent.getClauses().stream()
                        .map(this::toClauseInfo)
                        .collect(Collectors.toList());
                context.setClauses(clauses);
            } else {
                ParsedContract parsedContract = contractClauseParser.parse(context.getContent());
                List<ClauseInfo> clauses = parsedContract.getClauses().stream()
                        .map(this::toClauseInfo)
                        .collect(Collectors.toList());
                context.setClauses(clauses);

                try {
                    clauseGraphService.buildGraph(context.getTaskId(), parsedContract);
                } catch (Exception e) {
                    log.warn("Failed to build clause graph for taskId={}: {}", context.getTaskId(), e.getMessage());
                }
            }
        }

        QuickFilterResult filterResult = quickFilterService.filter(
                context.getContent(), context.getCustomRules());
        List<ReviewPipelineResult.MissingElementInfo> missingElements = new ArrayList<>();
        for (String ms : filterResult.getMissingStatements()) {
            missingElements.add(ReviewPipelineResult.MissingElementInfo.builder()
                    .cardCategory(CardCategory.FORMAT_ELEMENTS.getCode())
                    .element(ms)
                    .requirement("企业必备声明缺失")
                    .severity("major")
                    .suggestion("请添加必备声明: \"" + ms + "\"")
                    .build());
        }

        AssembledContext assembledContext = contextAssembler.assemble(context);
        context.setLawArticles(assembledContext.getRelevantArticles());
        if (context.getCustomRules() == null || context.getCustomRules().isEmpty()) {
            context.setCustomRules(assembledContext.getCustomRules());
        }
        if (context.getCases() == null || context.getCases().isEmpty()) {
            context.setCases(assembledContext.getCases());
        }
        context.setTemplateDiffResults(assembledContext.getTemplateDiffResults());

        List<ReviewIssue> allIssues;
        int contentLength = context.getContent() != null ? context.getContent().length() : 0;

        if (contentLength < SMALL_DOC_THRESHOLD) {
            allIssues = runAgentsSinglePass(context, applicableCards);
        } else if (contentLength <= MEDIUM_DOC_THRESHOLD) {
            allIssues = runAgentsMediumDoc(context, applicableCards);
        } else {
            allIssues = runAgentsLargeDoc(context, applicableCards);
        }

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

        if (context.getContent() != null && !context.getContent().isEmpty()) {
            for (int i = 0; i < allIssues.size(); i++) {
                allIssues.set(i, spanLocator.locate(allIssues.get(i), context.getContent()));
            }
        }

        List<ReviewIssue> verifiedIssues = citationVerifier.verify(allIssues);

        List<String> missingElementNames = missingElements.stream()
                .map(ReviewPipelineResult.MissingElementInfo::getElement)
                .toList();
        RiskScoreCalculator.RiskScoreResult riskResult = riskScoreCalculator.calculate(
                verifiedIssues, missingElementNames);

        String overallVerdict = determineOverallVerdict(verifiedIssues);

        List<ReviewCardResultDO> cardResultEntities = new ArrayList<>();
        for (CardCategory cc : CardCategory.values()) {
            List<ReviewIssue> cardIssues = verifiedIssues.stream()
                    .filter(i -> i.getCardCategory() == cc.getCode())
                    .toList();
            String maxSev = cardIssues.stream()
                    .map(ReviewIssue::getSeverity)
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingInt(s -> switch (s.toLowerCase()) {
                        case "critical" -> 0; case "major" -> 1; case "minor" -> 2; default -> 3;
                    }))
                    .orElse(null);
            cardResultEntities.add(ReviewCardResultDO.builder()
                    .taskId(context.getTaskId())
                    .cardCategory(cc.getCode())
                    .cardName(cc.getTitle())
                    .issueCount(cardIssues.size())
                    .maxSeverity(maxSev)
                    .status(cardIssues.isEmpty() ? "clean" : "has_issues")
                    .agentName(cc.name() + "Agent")
                    .build());
        }

        long totalLatency = System.currentTimeMillis() - startTime;
        log.info("Pipeline completed for taskId={}, issues={}, riskScore={}, latencyMs={}",
                context.getTaskId(), verifiedIssues.size(), riskResult.score(), totalLatency);

        return ReviewPipelineResult.builder()
                .results(verifiedIssues)
                .missingElements(missingElements)
                .cardResults(cardResultEntities)
                .overallVerdict(overallVerdict)
                .riskScore(riskResult.score())
                .riskLevel(riskResult.level())
                .totalLatencyMs((int) totalLatency)
                .build();
    }

    private List<ReviewIssue> runAgentsSinglePass(ReviewContext context, Set<Integer> applicableCards) {
        List<ReviewCardAgent> applicableAgents = filterAgents(context, applicableCards);
        return executeAgents(applicableAgents, context);
    }

    private List<ReviewIssue> runAgentsMediumDoc(ReviewContext context, Set<Integer> applicableCards) {
        List<String> chunks = chunkContent(context.getContent(), DEFAULT_CHUNK_SIZE);
        List<ReviewIssue> allIssues = Collections.synchronizedList(new ArrayList<>());

        List<ReviewCardAgent> nonCrossClauseAgents = filterAgents(context, applicableCards).stream()
                .filter(agent -> !(agent instanceof com.review.module.agent.impl.CrossClauseAgent))
                .toList();

        List<CompletableFuture<Void>> chunkFutures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final String chunk = chunks.get(i);
            chunkFutures.add(CompletableFuture.runAsync(() -> {
                ReviewContext subContext = createSubContext(context, chunk);
                List<ReviewIssue> chunkIssues = executeAgents(nonCrossClauseAgents, subContext);
                for (ReviewIssue issue : chunkIssues) {
                    issue.setSegmentIndex(chunkIndex);
                }
                allIssues.addAll(chunkIssues);
            }, agentExecutor));
        }

        chunkFutures.forEach(CompletableFuture::join);

        List<ReviewCardAgent> crossClauseAgents = filterAgents(context, applicableCards).stream()
                .filter(agent -> agent instanceof com.review.module.agent.impl.CrossClauseAgent)
                .toList();
        if (!crossClauseAgents.isEmpty()) {
            allIssues.addAll(executeAgents(crossClauseAgents, context));
        }

        reindexSegments(allIssues);
        return new ArrayList<>(allIssues);
    }

    private List<ReviewIssue> runAgentsLargeDoc(ReviewContext context, Set<Integer> applicableCards) {
        List<ReviewIssue> allIssues = new ArrayList<>();

        Set<Integer> phase1Cards = Set.of(
                CardCategory.TEXT_BASICS.getCode(),
                CardCategory.REDLINES.getCode());
        List<ReviewCardAgent> phase1Agents = filterAgents(context, applicableCards).stream()
                .filter(agent -> phase1Cards.contains(agent.getCardCategory().getCode()))
                .toList();
        allIssues.addAll(executeAgents(phase1Agents, context));

        Set<Integer> phase2Cards = Set.of(
                CardCategory.FORMAT_ELEMENTS.getCode(),
                CardCategory.SEMANTIC_COMPLIANCE.getCode(),
                CardCategory.LOGIC_CLAUSES.getCode());
        List<ReviewCardAgent> phase2Agents = filterAgents(context, applicableCards).stream()
                .filter(agent -> phase2Cards.contains(agent.getCardCategory().getCode()))
                .toList();

        List<String> highRiskSegments = identifyHighRiskSegments(context);
        if (!highRiskSegments.isEmpty()) {
            for (String segment : highRiskSegments) {
                ReviewContext subContext = createSubContext(context, segment);
                allIssues.addAll(executeAgents(phase2Agents, subContext));
            }
        } else {
            List<String> chunks = chunkContent(context.getContent(), DEFAULT_CHUNK_SIZE);
            for (String chunk : chunks) {
                ReviewContext subContext = createSubContext(context, chunk);
                allIssues.addAll(executeAgents(phase2Agents, subContext));
            }
        }

        reindexSegments(allIssues);
        return allIssues;
    }

    private List<ReviewCardAgent> filterAgents(ReviewContext context, Set<Integer> applicableCards) {
        return cardAgents.stream()
                .filter(agent -> agent.isApplicable(context))
                .filter(agent -> applicableCards == null
                        || applicableCards.contains(agent.getCardCategory().getCode()))
                .toList();
    }

    private List<ReviewIssue> executeAgents(List<ReviewCardAgent> agents, ReviewContext context) {
        Map<Integer, List<ReviewIssue>> cardResults = new LinkedHashMap<>();
        List<ReviewIssue> allIssues = new ArrayList<>();

        List<CompletableFuture<AgentResult>> futures = agents.stream()
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

        return allIssues;
    }

    List<String> chunkContent(String content, int maxChunkSize) {
        if (content == null || content.isEmpty()) return Collections.emptyList();
        if (content.length() <= maxChunkSize) return List.of(content);

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() + 2 > maxChunkSize && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
                String overlap = getOverlapText(currentChunk.toString(), CHUNK_OVERLAP);
                currentChunk = new StringBuilder(overlap);
            }
            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }

    private ReviewContext createSubContext(ReviewContext parent, String chunkContent) {
        ReviewContext sub = ReviewContext.builder()
                .taskId(parent.getTaskId())
                .tenantId(parent.getTenantId())
                .content(chunkContent)
                .documentType(parent.getDocumentType())
                .contractType(parent.getContractType())
                .contentType(parent.getContentType())
                .productType(parent.getProductType())
                .channel(parent.getChannel())
                .lawArticles(parent.getLawArticles())
                .customRules(parent.getCustomRules())
                .cases(parent.getCases())
                .templateDiffResults(parent.getTemplateDiffResults())
                .build();

        ParsedContent parsed = contentParserFactory.getParser(parent.getDocumentType()).parse(chunkContent);
        sub.setSegments(parsed.getSegments());
        if (parsed.getClauses() != null && !parsed.getClauses().isEmpty()) {
            sub.setClauses(parsed.getClauses().stream()
                    .map(this::toClauseInfo)
                    .collect(Collectors.toList()));
        }

        return sub;
    }

    private List<String> identifyHighRiskSegments(ReviewContext context) {
        if (context.getContent() == null) return Collections.emptyList();

        List<String> highRiskKeywords = List.of(
                "违约", "赔偿", "处罚", "禁止", "不得", "责任",
                "风险", "损失", "终止", "解除", "保证", "担保");

        List<String> chunks = chunkContent(context.getContent(), DEFAULT_CHUNK_SIZE);
        List<String> highRisk = new ArrayList<>();

        for (String chunk : chunks) {
            int hitCount = 0;
            for (String keyword : highRiskKeywords) {
                if (chunk.contains(keyword)) hitCount++;
            }
            if (hitCount >= 2) {
                highRisk.add(chunk);
            }
        }

        return highRisk;
    }

    private void reindexSegments(List<ReviewIssue> issues) {
        for (int i = 0; i < issues.size(); i++) {
            if (issues.get(i).getSegmentIndex() == null) {
                issues.get(i).setSegmentIndex(i);
            }
        }
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
