package com.review.module.registry;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DocumentTypeRegistry {

    private final Map<String, DocumentTypeConfig> configs = new LinkedHashMap<>();

    public DocumentTypeRegistry() {
        register(DocumentTypeConfig.builder()
                .documentType("marketing")
                .displayName("营销材料")
                .applicableCards(Set.of(1, 2, 3, 4))
                .defaultParser("text")
                .requiredElements(List.of("风险提示", "免责声明", "过往业绩不代表未来表现"))
                .maxContentLength(50000)
                .enableCrossClause(false)
                .build());

        register(DocumentTypeConfig.builder()
                .documentType("contract")
                .displayName("合同")
                .applicableCards(Set.of(1, 2, 3, 4, 5))
                .defaultParser("contract")
                .requiredElements(List.of("合同编号", "甲方", "乙方", "签署日期", "争议解决"))
                .maxContentLength(100000)
                .enableCrossClause(true)
                .build());

        register(DocumentTypeConfig.builder()
                .documentType("prospectus")
                .displayName("招股说明书")
                .applicableCards(Set.of(1, 2, 3, 4))
                .defaultParser("text")
                .requiredElements(List.of("风险揭示", "投资策略", "费率说明"))
                .maxContentLength(200000)
                .enableCrossClause(false)
                .build());

        register(DocumentTypeConfig.builder()
                .documentType("report")
                .displayName("报告")
                .applicableCards(Set.of(1, 2, 3, 4))
                .defaultParser("text")
                .requiredElements(List.of())
                .maxContentLength(100000)
                .enableCrossClause(false)
                .build());

        register(DocumentTypeConfig.builder()
                .documentType("other")
                .displayName("其他")
                .applicableCards(Set.of(1, 2, 3, 4))
                .defaultParser("text")
                .requiredElements(List.of())
                .maxContentLength(100000)
                .enableCrossClause(false)
                .build());
    }

    public void register(DocumentTypeConfig config) {
        configs.put(config.getDocumentType().toLowerCase(), config);
    }

    public DocumentTypeConfig getConfig(String documentType) {
        if (documentType == null) {
            return configs.get("other");
        }
        return configs.getOrDefault(documentType.toLowerCase(), configs.get("other"));
    }

    public Map<String, DocumentTypeConfig> getAllConfigs() {
        return Collections.unmodifiableMap(configs);
    }
}
