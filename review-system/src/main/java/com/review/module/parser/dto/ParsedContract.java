package com.review.module.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedContract {

    private List<ParsedClause> clauses;
    private Map<String, String> definitions;
    private List<CrossReference> crossReferences;
}
