package com.review.module.parser;

import com.review.module.parser.dto.ParsedContent;

public interface ContentParser {
    ParsedContent parse(String content);
}
