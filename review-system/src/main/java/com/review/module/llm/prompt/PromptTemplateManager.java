package com.review.module.llm.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PromptTemplateManager {

    private static final String REVIEW_PROMPT_TEMPLATE = """
            你是一个专业的合规审查助手。请审查以下内容并以JSON格式返回审查结果。
            
            ## 审查内容
            {content}
            
            ## 相关法规
            {lawArticles}
            
            ## 自定义规则
            {customRules}
            
            ## 参考案例
            {cases}
            
            ## 文档类型: {documentType}
            ## 内容类型: {contentType}
            
            请以如下JSON格式返回结果:
            {
              "issues": [
                {
                  "verdict": "VIOLATION|COMPLIANT|NEEDS_REVIEW",
                  "confidence": 0.0-1.0,
                  "issueType": "问题类型",
                  "severity": "CRITICAL|MAJOR|MINOR|INFO",
                  "description": "问题描述",
                  "suggestion": "修改建议",
                  "suggestionType": "TIP|REVISION|NEGOTIATION_POINT",
                  "citedLawName": "引用法规名",
                  "citedArticleCode": "法条编号"
                }
              ],
              "summary": "审查总结"
            }
            """;

    private static final String CONTRACT_REVIEW_TEMPLATE = """
            你是一个专业的合同审查助手。请审查以下合同条款并以JSON格式返回审查结果。
            
            ## 合同条款
            {clauseText}
            
            ## 合同类型: {contractType}
            
            ## 相关法规
            {lawArticles}
            
            ## 自定义规则
            {customRules}
            
            请重点检查:
            1. 权责对等性
            2. 违约金合理性
            3. 赔偿上限设置
            4. 解除条件对等性
            5. 法律条款引用准确性
            
            请以JSON格式返回结果。
            """;

    public String buildReviewPrompt(Map<String, String> variables) {
        return substituteVariables(REVIEW_PROMPT_TEMPLATE, variables);
    }

    public String buildContractReviewPrompt(Map<String, String> variables) {
        return substituteVariables(CONTRACT_REVIEW_TEMPLATE, variables);
    }

    public String substituteVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
