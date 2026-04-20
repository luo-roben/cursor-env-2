package com.review.module.parser.impl;

import com.review.module.parser.ContractClauseParser;
import com.review.module.parser.dto.CrossReference;
import com.review.module.parser.dto.ParsedClause;
import com.review.module.parser.dto.ParsedContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ContractClauseParserImpl implements ContractClauseParser {

    private static final Pattern CHINESE_CLAUSE_PATTERN =
            Pattern.compile("第([一二三四五六七八九十百零]+)条\\s*[：:]?\\s*(.*)");

    private static final Pattern NUMBERED_CLAUSE_PATTERN =
            Pattern.compile("^(\\d+(?:\\.\\d+)*)\\s*[、.．]?\\s*(.*)");

    private static final Pattern DEFINITION_PATTERN =
            Pattern.compile("(?:本合同所称|本协议所称)[\u201c\u201d\"']?([^\u201c\u201d\"'\uff0c,\u3002.]+)[\u201c\u201d\"']?\\s*(?:是指|指)\\s*(.+?)(?:[\u3002\uff1b;]|$)");

    private static final Pattern CROSS_REF_PATTERN =
            Pattern.compile("(?:依照|按照|根据|参见|见)本(?:合同|协议)第([一二三四五六七八九十百零\\d.]+)条");

    private static final Map<String, List<String>> CLAUSE_TYPE_KEYWORDS = Map.of(
            "DEFINITION", List.of("定义", "释义", "术语", "是指", "所称"),
            "OBLIGATION", List.of("应当", "必须", "应该", "有义务", "负责"),
            "RIGHT", List.of("有权", "可以", "享有", "权利"),
            "LIABILITY", List.of("违约", "赔偿", "罚款", "损失", "违约金"),
            "TERMINATION", List.of("解除", "终止", "到期", "届满"),
            "DISPUTE", List.of("争议", "仲裁", "诉讼", "管辖", "调解")
    );

    @Override
    public ParsedContract parse(String content) {
        if (content == null || content.isEmpty()) {
            return ParsedContract.builder()
                    .clauses(Collections.emptyList())
                    .definitions(Collections.emptyMap())
                    .crossReferences(Collections.emptyList())
                    .build();
        }

        List<ParsedClause> clauses = parseClauses(content);
        Map<String, String> definitions = extractDefinitions(content);
        List<CrossReference> crossReferences = detectCrossReferences(content, clauses);

        return ParsedContract.builder()
                .clauses(clauses)
                .definitions(definitions)
                .crossReferences(crossReferences)
                .build();
    }

    private List<ParsedClause> parseClauses(String content) {
        List<ParsedClause> clauses = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder currentClauseText = new StringBuilder();
        String currentClauseNumber = null;
        String currentClauseTitle = null;
        int currentClauseOffset = 0;
        int currentOffset = 0;

        for (String line : lines) {
            Matcher chineseMatcher = CHINESE_CLAUSE_PATTERN.matcher(line.trim());
            Matcher numberedMatcher = NUMBERED_CLAUSE_PATTERN.matcher(line.trim());

            boolean isNewClause = chineseMatcher.matches() || numberedMatcher.matches();

            if (isNewClause) {
                if (currentClauseNumber != null) {
                    String clauseText = currentClauseText.toString().trim();
                    clauses.add(ParsedClause.builder()
                            .clauseNumber(currentClauseNumber)
                            .clauseTitle(currentClauseTitle)
                            .clauseText(clauseText)
                            .clauseType(inferClauseType(clauseText, currentClauseTitle))
                            .charOffset(currentClauseOffset)
                            .charLength(clauseText.length())
                            .subClauses(new ArrayList<>())
                            .build());
                }

                if (chineseMatcher.matches()) {
                    currentClauseNumber = chineseNumberToArabic(chineseMatcher.group(1));
                    currentClauseTitle = chineseMatcher.group(2).trim();
                } else {
                    currentClauseNumber = numberedMatcher.group(1);
                    currentClauseTitle = numberedMatcher.group(2).trim();
                }
                currentClauseText = new StringBuilder();
                currentClauseOffset = currentOffset;
            }

            if (currentClauseNumber != null) {
                currentClauseText.append(line).append("\n");
            }

            currentOffset += line.length() + 1;
        }

        if (currentClauseNumber != null) {
            String clauseText = currentClauseText.toString().trim();
            clauses.add(ParsedClause.builder()
                    .clauseNumber(currentClauseNumber)
                    .clauseTitle(currentClauseTitle)
                    .clauseText(clauseText)
                    .clauseType(inferClauseType(clauseText, currentClauseTitle))
                    .charOffset(currentClauseOffset)
                    .charLength(clauseText.length())
                    .subClauses(new ArrayList<>())
                    .build());
        }

        return clauses;
    }

    private Map<String, String> extractDefinitions(String content) {
        Map<String, String> definitions = new LinkedHashMap<>();
        Matcher matcher = DEFINITION_PATTERN.matcher(content);
        while (matcher.find()) {
            definitions.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return definitions;
    }

    private List<CrossReference> detectCrossReferences(String content, List<ParsedClause> clauses) {
        List<CrossReference> refs = new ArrayList<>();
        
        for (ParsedClause clause : clauses) {
            Matcher matcher = CROSS_REF_PATTERN.matcher(clause.getClauseText());
            while (matcher.find()) {
                String targetRef = matcher.group(1);
                refs.add(CrossReference.builder()
                        .sourceClause(clause.getClauseNumber())
                        .targetClause(targetRef)
                        .referenceText(matcher.group())
                        .charOffset(clause.getCharOffset() + matcher.start())
                        .build());
            }
        }
        return refs;
    }

    private String inferClauseType(String text, String title) {
        String combined = (title != null ? title : "") + " " + text;
        int maxScore = 0;
        String bestType = "GENERAL";

        for (Map.Entry<String, List<String>> entry : CLAUSE_TYPE_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (combined.contains(keyword)) score++;
            }
            if (score > maxScore) {
                maxScore = score;
                bestType = entry.getKey();
            }
        }
        return bestType;
    }

    private String chineseNumberToArabic(String chinese) {
        Map<Character, Integer> digitMap = Map.ofEntries(
                Map.entry('零', 0), Map.entry('一', 1), Map.entry('二', 2),
                Map.entry('三', 3), Map.entry('四', 4), Map.entry('五', 5),
                Map.entry('六', 6), Map.entry('七', 7), Map.entry('八', 8),
                Map.entry('九', 9), Map.entry('十', 10), Map.entry('百', 100)
        );

        if (chinese.length() == 1) {
            return String.valueOf(digitMap.getOrDefault(chinese.charAt(0), 0));
        }

        int result = 0;
        int current = 0;
        for (char c : chinese.toCharArray()) {
            Integer val = digitMap.get(c);
            if (val == null) continue;
            if (val >= 10) {
                if (current == 0) current = 1;
                result += current * val;
                current = 0;
            } else {
                current = val;
            }
        }
        result += current;
        return String.valueOf(result);
    }
}
