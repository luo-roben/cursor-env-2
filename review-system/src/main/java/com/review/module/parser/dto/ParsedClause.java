package com.review.module.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedClause {

    private String clauseNumber;
    private String clauseTitle;
    private String clauseText;
    private String clauseType;
    private int charOffset;
    private int charLength;
    private List<ParsedClause> subClauses;
}
