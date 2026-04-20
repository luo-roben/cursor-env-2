package com.review.module.llm.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRouter {

    private final MockChatModelProvider mockChatModelProvider;

    public ChatModelProvider route(String modelName) {
        log.debug("Routing model request for: {}", modelName);
        return mockChatModelProvider;
    }

    public String generate(String modelName, String prompt) {
        ChatModelProvider provider = route(modelName);
        return provider.generate(modelName, prompt);
    }
}
