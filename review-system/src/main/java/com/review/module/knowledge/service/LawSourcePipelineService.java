package com.review.module.knowledge.service;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.module.knowledge.entity.LawArticleDO;
import com.review.module.knowledge.entity.LawSourceDO;
import com.review.module.knowledge.repository.LawArticleRepository;
import com.review.module.knowledge.repository.LawSourceRepository;
import com.review.module.llm.model.ModelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawSourcePipelineService {

    private final LawSourceRepository lawSourceRepository;
    private final LawArticleRepository lawArticleRepository;
    private final ModelRouter modelRouter;

    @Transactional
    public void triggerParsing(Long lawSourceId) {
        LawSourceDO source = lawSourceRepository.findById(lawSourceId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_SOURCE_NOT_FOUND));

        if (!"pending".equals(source.getParseStatus()) && !"failed".equals(source.getParseStatus())) {
            throw new ServiceException(ErrorCode.BAD_REQUEST,
                    "法规来源解析状态不允许触发解析, 当前状态: " + source.getParseStatus());
        }

        source.setParseStatus("parsing");
        lawSourceRepository.save(source);

        try {
            String prompt = buildParsePrompt(source);
            String response = modelRouter.generate(null, prompt);
            List<ArticleData> articles = parseResponse(response, source);

            for (ArticleData article : articles) {
                LawArticleDO articleDO = LawArticleDO.builder()
                        .sourceId(source.getId())
                        .lawName(source.getTitle())
                        .articleId(article.articleId)
                        .originalText(article.text)
                        .normType(article.normType != null ? article.normType : "定义")
                        .authorityLevel(determineAuthorityLevel(source.getDocType()))
                        .status("draft")
                        .build();
                lawArticleRepository.save(articleDO);
            }

            source.setParseStatus("parsed");
            lawSourceRepository.save(source);
            log.info("Parsed {} articles from lawSourceId={}", articles.size(), lawSourceId);

        } catch (Exception e) {
            log.error("Failed to parse lawSourceId={}: {}", lawSourceId, e.getMessage(), e);
            source.setParseStatus("failed");
            lawSourceRepository.save(source);
            throw new ServiceException(ErrorCode.AI_SERVICE_ERROR, "法规解析失败: " + e.getMessage());
        }
    }

    @Transactional
    public void confirmAll(Long lawSourceId, Long confirmedBy) {
        LawSourceDO source = lawSourceRepository.findById(lawSourceId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_SOURCE_NOT_FOUND));

        List<LawArticleDO> draftArticles = lawArticleRepository.findBySourceIdAndStatus(source.getId(), "draft");
        LocalDateTime now = LocalDateTime.now();

        for (LawArticleDO article : draftArticles) {
            article.setStatus("published");
            article.setConfirmedBy(confirmedBy);
            article.setConfirmedAt(now);
        }
        lawArticleRepository.saveAll(draftArticles);

        source.setParseStatus("confirmed");
        lawSourceRepository.save(source);

        log.info("Confirmed {} articles for lawSourceId={}", draftArticles.size(), lawSourceId);
    }

    private String buildParsePrompt(LawSourceDO source) {
        return "请将以下法规全文拆分为独立的法条。每个法条请用以下格式输出：\n" +
                "【法条编号】条文编号\n" +
                "【法条内容】条文内容\n" +
                "【规范类型】禁止/义务/权利/定义/程序\n\n" +
                "法规标题: " + source.getTitle() + "\n" +
                "法规全文:\n" + (source.getFullText() != null ? source.getFullText() : "");
    }

    private List<ArticleData> parseResponse(String response, LawSourceDO source) {
        List<ArticleData> articles = new ArrayList<>();

        if (response == null || response.isEmpty()) {
            return articles;
        }

        Pattern articlePattern = Pattern.compile(
                "【法条编号】(.+?)\\s*(?:\\n|$).*?【法条内容】(.+?)\\s*(?:【规范类型】(.+?)\\s*)?(?=【法条编号】|$)",
                Pattern.DOTALL);

        Matcher matcher = articlePattern.matcher(response);
        while (matcher.find()) {
            articles.add(new ArticleData(
                    matcher.group(1).trim(),
                    matcher.group(2).trim(),
                    matcher.group(3) != null ? matcher.group(3).trim() : "定义"
            ));
        }

        // If pattern didn't match, try simple split by article numbering
        if (articles.isEmpty()) {
            String[] lines = response.split("\n");
            StringBuilder currentText = new StringBuilder();
            String currentId = null;
            int counter = 1;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (trimmed.matches("^第[一二三四五六七八九十百千0-9]+条.*")) {
                    if (currentId != null) {
                        articles.add(new ArticleData(currentId, currentText.toString().trim(), "定义"));
                    }
                    currentId = "第" + counter + "条";
                    currentText = new StringBuilder(trimmed);
                    counter++;
                } else if (currentId != null) {
                    currentText.append("\n").append(trimmed);
                }
            }
            if (currentId != null) {
                articles.add(new ArticleData(currentId, currentText.toString().trim(), "定义"));
            }
        }

        return articles;
    }

    private int determineAuthorityLevel(String docType) {
        if (docType == null) return 3;
        return switch (docType) {
            case "法律" -> 1;
            case "行政法规" -> 2;
            case "部门规章" -> 3;
            case "规范性文件" -> 4;
            case "自律规则" -> 5;
            case "监管问答" -> 6;
            default -> 3;
        };
    }

    private record ArticleData(String articleId, String text, String normType) {}
}
