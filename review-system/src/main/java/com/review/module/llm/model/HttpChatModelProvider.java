package com.review.module.llm.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "review.llm.api-url")
public class HttpChatModelProvider implements ChatModelProvider {

    @Value("${review.llm.api-url:}")
    private String apiUrl;

    @Value("${review.llm.api-key:}")
    private String apiKey;

    @Value("${review.llm.default-model:gpt-3.5-turbo}")
    private String defaultModel;

    @Value("${review.llm.temperature:0.0}")
    private double temperature;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String generate(String modelName, String prompt) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("LLM API URL is not configured");
        }

        try {
            String model = (modelName != null && !modelName.isBlank() && !"http".equals(modelName))
                    ? modelName : defaultModel;

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(120));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("LLM API returned status " + response.statusCode() +
                        ": " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode choices = responseJson.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
                content = choices.get(0).path("text").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
            }

            JsonNode directContent = responseJson.path("content");
            if (directContent.isTextual()) {
                return directContent.asText();
            }

            log.warn("Could not parse LLM API response, returning raw body");
            return response.body();
        } catch (Exception e) {
            log.error("HTTP LLM API call failed: {}", e.getMessage());
            throw new RuntimeException("LLM API call failed: " + e.getMessage(), e);
        }
    }
}
