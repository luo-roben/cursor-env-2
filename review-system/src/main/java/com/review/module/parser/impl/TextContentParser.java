package com.review.module.parser.impl;

import com.review.module.parser.ContentParser;
import com.review.module.parser.dto.ParsedContent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component("textContentParser")
public class TextContentParser implements ContentParser {

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

        return ParsedContent.builder()
                .segments(segments)
                .build();
    }
}
