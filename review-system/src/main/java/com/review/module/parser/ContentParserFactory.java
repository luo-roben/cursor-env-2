package com.review.module.parser;

import com.review.module.registry.DocumentTypeConfig;
import com.review.module.registry.DocumentTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ContentParserFactory {

    private final Map<String, ContentParser> parsers;
    private final DocumentTypeRegistry documentTypeRegistry;

    public ContentParserFactory(Map<String, ContentParser> parsers,
                                DocumentTypeRegistry documentTypeRegistry) {
        this.parsers = parsers;
        this.documentTypeRegistry = documentTypeRegistry;
    }

    public ContentParser getParser(String documentType) {
        DocumentTypeConfig config = documentTypeRegistry.getConfig(documentType);
        String parserName = config != null && config.getDefaultParser() != null
                ? config.getDefaultParser()
                : "text";

        String beanName = parserName + "ContentParser";
        ContentParser parser = parsers.get(beanName);
        if (parser != null) {
            return parser;
        }

        return parsers.getOrDefault("textContentParser", new FallbackParser());
    }

    private static class FallbackParser implements ContentParser {
        @Override
        public com.review.module.parser.dto.ParsedContent parse(String content) {
            if (content == null || content.isEmpty()) {
                return com.review.module.parser.dto.ParsedContent.builder()
                        .segments(java.util.Collections.emptyList())
                        .build();
            }
            String[] paragraphs = content.split("\n\n+");
            java.util.List<String> segments = new java.util.ArrayList<>();
            for (String p : paragraphs) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    segments.add(trimmed);
                }
            }
            return com.review.module.parser.dto.ParsedContent.builder()
                    .segments(segments)
                    .build();
        }
    }
}
