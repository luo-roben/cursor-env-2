package com.review.infrastructure.embedding;

public interface EmbeddingService {
    float[] embed(String text);
    int getDimension();
}
