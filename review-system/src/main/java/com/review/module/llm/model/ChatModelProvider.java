package com.review.module.llm.model;

public interface ChatModelProvider {

    String generate(String modelName, String prompt);
}
