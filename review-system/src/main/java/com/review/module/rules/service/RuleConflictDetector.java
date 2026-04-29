package com.review.module.rules.service;

import com.review.module.rules.dto.RuleConflict;
import com.review.module.rules.entity.TenantCustomRuleDO;
import com.review.module.rules.repository.TenantCustomRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleConflictDetector {

    private final TenantCustomRuleRepository tenantCustomRuleRepository;

    public List<RuleConflict> detectConflicts(Long tenantId) {
        List<TenantCustomRuleDO> enabledRules = tenantCustomRuleRepository.findByTenantIdAndEnabledTrue(tenantId);
        List<RuleConflict> conflicts = new ArrayList<>();

        for (int i = 0; i < enabledRules.size(); i++) {
            for (int j = i + 1; j < enabledRules.size(); j++) {
                TenantCustomRuleDO r1 = enabledRules.get(i);
                TenantCustomRuleDO r2 = enabledRules.get(j);

                // Check contradiction: banned_word content matches required_statement content
                if (isContradiction(r1, r2)) {
                    conflicts.add(RuleConflict.builder()
                            .ruleId1(r1.getId())
                            .ruleId2(r2.getId())
                            .conflictType("contradiction")
                            .description("禁用词规则与必备声明规则内容冲突: '" + r1.getContent() + "' vs '" + r2.getContent() + "'")
                            .build());
                }

                // Check overlap: two banned_word rules with same content
                if (isOverlap(r1, r2)) {
                    conflicts.add(RuleConflict.builder()
                            .ruleId1(r1.getId())
                            .ruleId2(r2.getId())
                            .conflictType("overlap")
                            .description("两条相同类型规则内容重复: '" + r1.getContent() + "'")
                            .build());
                }

                // Check semantic conflict for natural_language rules
                String semanticConflict = detectSemanticConflict(r1, r2);
                if (semanticConflict != null) {
                    conflicts.add(RuleConflict.builder()
                            .ruleId1(r1.getId())
                            .ruleId2(r2.getId())
                            .conflictType("semantic")
                            .description(semanticConflict)
                            .build());
                }
            }
        }

        log.info("Detected {} rule conflicts for tenantId={}", conflicts.size(), tenantId);
        return conflicts;
    }

    private boolean isContradiction(TenantCustomRuleDO r1, TenantCustomRuleDO r2) {
        String type1 = r1.getRuleType() != null ? r1.getRuleType().toUpperCase() : "";
        String type2 = r2.getRuleType() != null ? r2.getRuleType().toUpperCase() : "";
        String content1 = r1.getContent() != null ? r1.getContent().trim() : "";
        String content2 = r2.getContent() != null ? r2.getContent().trim() : "";

        return (("BANNED_WORD".equals(type1) && "REQUIRED_STATEMENT".equals(type2)) ||
                ("REQUIRED_STATEMENT".equals(type1) && "BANNED_WORD".equals(type2)))
                && (content1.contains(content2) || content2.contains(content1));
    }

    private boolean isOverlap(TenantCustomRuleDO r1, TenantCustomRuleDO r2) {
        if (r1.getRuleType() == null || r2.getRuleType() == null) return false;
        if (!r1.getRuleType().equalsIgnoreCase(r2.getRuleType())) return false;
        if ("BANNED_WORD".equalsIgnoreCase(r1.getRuleType())) {
            String content1 = r1.getContent() != null ? r1.getContent().trim() : "";
            String content2 = r2.getContent() != null ? r2.getContent().trim() : "";
            return content1.equalsIgnoreCase(content2);
        }
        return false;
    }

    private String detectSemanticConflict(TenantCustomRuleDO r1, TenantCustomRuleDO r2) {
        String type1 = r1.getRuleType() != null ? r1.getRuleType().toUpperCase() : "";
        String type2 = r2.getRuleType() != null ? r2.getRuleType().toUpperCase() : "";

        if (!"NATURAL_LANGUAGE".equals(type1) || !"NATURAL_LANGUAGE".equals(type2)) {
            return null;
        }

        String c1 = r1.getContent() != null ? r1.getContent() : "";
        String c2 = r2.getContent() != null ? r2.getContent() : "";

        // Check for contradictory keywords: "必须包含X" vs "禁止X"
        String[][] opposites = {
                {"必须包含", "禁止"},
                {"必须", "不得"},
                {"应当", "不应"},
                {"需要", "禁止"},
                {"要求", "禁止"}
        };

        for (String[] pair : opposites) {
            if ((c1.contains(pair[0]) && c2.contains(pair[1])) ||
                    (c1.contains(pair[1]) && c2.contains(pair[0]))) {
                // Extract the subject from both rules and check overlap
                String subject1 = extractSubject(c1);
                String subject2 = extractSubject(c2);
                if (subject1 != null && subject2 != null &&
                        (subject1.contains(subject2) || subject2.contains(subject1))) {
                    return "自然语言规则语义冲突: '" + c1 + "' 与 '" + c2 + "' 可能存在矛盾";
                }
            }
        }

        return null;
    }

    private String extractSubject(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] keywords = {"必须包含", "禁止", "必须", "不得", "应当", "不应", "需要", "要求"};
        for (String kw : keywords) {
            int idx = text.indexOf(kw);
            if (idx >= 0) {
                String after = text.substring(idx + kw.length()).trim();
                if (!after.isEmpty()) {
                    return after.length() > 20 ? after.substring(0, 20) : after;
                }
            }
        }
        return text.length() > 20 ? text.substring(0, 20) : text;
    }
}
