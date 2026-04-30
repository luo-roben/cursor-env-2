package com.review.module.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.module.registry.entity.DocTypeRegistryDO;
import com.review.module.registry.repository.DocTypeRegistryRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class DocumentTypeRegistry {

    private final Map<String, DocumentTypeConfig> configs = new LinkedHashMap<>();

    @Autowired(required = false)
    private DocTypeRegistryRepository docTypeRegistryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentTypeRegistry() {
        registerDefaults();
    }

    @PostConstruct
    public void init() {
        loadFromDatabase();
    }

    private void registerDefaults() {
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

    private void loadFromDatabase() {
        if (docTypeRegistryRepository == null) {
            log.debug("DocTypeRegistryRepository not available, using in-memory defaults");
            return;
        }
        try {
            List<DocTypeRegistryDO> dbConfigs = docTypeRegistryRepository.findByEnabledTrue();
            if (dbConfigs.isEmpty()) {
                log.debug("No doc type configs in DB, using in-memory defaults");
                return;
            }
            for (DocTypeRegistryDO dbConfig : dbConfigs) {
                try {
                    DocumentTypeConfig config = toConfig(dbConfig);
                    register(config);
                } catch (Exception e) {
                    log.warn("Failed to parse doc type config for {}: {}", dbConfig.getDocumentType(), e.getMessage());
                }
            }
            log.info("Loaded {} document type configs from database", dbConfigs.size());
        } catch (Exception e) {
            log.warn("Failed to load doc type configs from database, using in-memory defaults: {}", e.getMessage());
        }
    }

    public void refresh() {
        configs.clear();
        registerDefaults();
        loadFromDatabase();
    }

    private DocumentTypeConfig toConfig(DocTypeRegistryDO dbConfig) {
        Set<Integer> cards = new HashSet<>();
        try {
            List<Integer> cardList = objectMapper.readValue(dbConfig.getApplicableCards(), new TypeReference<>() {});
            cards.addAll(cardList);
        } catch (Exception e) {
            log.warn("Failed to parse applicable_cards JSON for {}: {}", dbConfig.getDocumentType(), e.getMessage());
        }

        List<String> elements = List.of();
        try {
            if (dbConfig.getRequiredElements() != null && !dbConfig.getRequiredElements().isEmpty()) {
                elements = objectMapper.readValue(dbConfig.getRequiredElements(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse required_elements JSON for {}: {}", dbConfig.getDocumentType(), e.getMessage());
        }

        return DocumentTypeConfig.builder()
                .documentType(dbConfig.getDocumentType())
                .displayName(dbConfig.getDisplayName())
                .applicableCards(cards)
                .defaultParser(dbConfig.getDefaultParser())
                .requiredElements(elements)
                .maxContentLength(dbConfig.getMaxContentLength() != null ? dbConfig.getMaxContentLength() : 100000)
                .enableCrossClause(Boolean.TRUE.equals(dbConfig.getEnableCrossClause()))
                .build();
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
