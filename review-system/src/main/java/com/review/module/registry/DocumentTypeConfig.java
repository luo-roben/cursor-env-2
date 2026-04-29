package com.review.module.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeConfig {

    private String documentType;
    private String displayName;
    private Set<Integer> applicableCards;
    private String defaultParser;
    private List<String> requiredElements;
    private int maxContentLength;
    private boolean enableCrossClause;
}
