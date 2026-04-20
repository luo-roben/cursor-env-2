package com.review.module.llm.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ModelRouter {

    private final MockChatModelProvider mockChatModelProvider;
    private final Map<String, ChatModelProvider> providerMap = new ConcurrentHashMap<>();

    @Value("${review.llm.default-model:mock}")
    private String defaultModel;

    public ModelRouter(MockChatModelProvider mockChatModelProvider,
                       List<ChatModelProvider> providers) {
        this.mockChatModelProvider = mockChatModelProvider;
        for (ChatModelProvider provider : providers) {
            String name = resolveProviderName(provider);
            providerMap.put(name, provider);
            log.info("Registered ChatModelProvider: {}", name);
        }
    }

    public ChatModelProvider route(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            modelName = defaultModel;
        }

        log.debug("Routing model request for: {}", modelName);

        if ("mock".equalsIgnoreCase(modelName)) {
            return mockChatModelProvider;
        }

        ChatModelProvider provider = providerMap.get(modelName);
        if (provider != null) {
            return provider;
        }

        for (Map.Entry<String, ChatModelProvider> entry : providerMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(modelName)) {
                return entry.getValue();
            }
        }

        log.warn("No provider found for model '{}', falling back to mock provider", modelName);
        return mockChatModelProvider;
    }

    public String generate(String modelName, String prompt) {
        ChatModelProvider provider = route(modelName);
        String providerName = resolveProviderName(provider);

        try {
            log.debug("Generating with provider '{}' for model '{}'", providerName, modelName);
            return provider.generate(modelName, prompt);
        } catch (Exception e) {
            log.warn("Generation failed with provider '{}' for model '{}': {}. Falling back to mock.",
                    providerName, modelName, e.getMessage());
            if (provider != mockChatModelProvider) {
                try {
                    return mockChatModelProvider.generate(modelName, prompt);
                } catch (Exception fallbackEx) {
                    log.error("Fallback mock provider also failed: {}", fallbackEx.getMessage());
                    throw fallbackEx;
                }
            }
            throw e;
        }
    }

    private String resolveProviderName(ChatModelProvider provider) {
        if (provider instanceof MockChatModelProvider) {
            return "mock";
        }
        if (provider instanceof HttpChatModelProvider) {
            return "http";
        }
        return provider.getClass().getSimpleName();
    }
}
