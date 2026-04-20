package com.review.module.parser;

import com.review.module.parser.dto.ParsedContract;

public interface ContractClauseParser {

    ParsedContract parse(String content);
}
