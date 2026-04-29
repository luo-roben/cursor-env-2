package com.review.module.parser.impl;

import com.review.module.parser.ContentParser;
import com.review.module.parser.ContractClauseParser;
import com.review.module.parser.dto.ParsedContent;
import com.review.module.parser.dto.ParsedContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component("contractContentParser")
@RequiredArgsConstructor
public class ContractContentParser implements ContentParser {

    private final ContractClauseParser contractClauseParser;

    @Override
    public ParsedContent parse(String content) {
        if (content == null || content.isEmpty()) {
            return ParsedContent.builder()
                    .segments(Collections.emptyList())
                    .build();
        }

        String[] paragraphs = content.split("\n\n+");
        List<String> segments = new ArrayList<>();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }

        ParsedContract parsedContract = contractClauseParser.parse(content);

        return ParsedContent.builder()
                .segments(segments)
                .clauses(parsedContract.getClauses())
                .definitions(parsedContract.getDefinitions())
                .crossReferences(parsedContract.getCrossReferences())
                .build();
    }
}
