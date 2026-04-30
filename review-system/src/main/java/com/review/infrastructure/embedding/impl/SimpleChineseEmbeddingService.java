package com.review.infrastructure.embedding.impl;

import com.review.infrastructure.embedding.EmbeddingService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SimpleChineseEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 128;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSION];
        if (text == null || text.isEmpty()) {
            return vector;
        }

        Map<Integer, Integer> bigramFreq = new HashMap<>();
        for (int i = 0; i < text.length() - 1; i++) {
            char c1 = text.charAt(i);
            char c2 = text.charAt(i + 1);
            int bigramHash = Character.hashCode(c1) * 31 + Character.hashCode(c2);
            int pos = Math.floorMod(bigramHash, DIMENSION);
            bigramFreq.merge(pos, 1, Integer::sum);
        }

        if (text.length() == 1) {
            int pos = Math.floorMod(Character.hashCode(text.charAt(0)), DIMENSION);
            vector[pos] = 1.0f;
            return vector;
        }

        for (Map.Entry<Integer, Integer> entry : bigramFreq.entrySet()) {
            vector[entry.getKey()] = entry.getValue().floatValue();
        }

        float norm = 0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIMENSION; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
}
