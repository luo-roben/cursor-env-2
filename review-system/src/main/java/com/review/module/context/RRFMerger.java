package com.review.module.context;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RRFMerger {

    private static final int K = 60;

    public <T> List<T> merge(List<List<T>> rankedLists, Function<T, String> idExtractor) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, T> items = new HashMap<>();

        for (List<T> list : rankedLists) {
            for (int rank = 0; rank < list.size(); rank++) {
                T item = list.get(rank);
                String id = idExtractor.apply(item);
                scores.merge(id, 1.0 / (K + rank + 1), Double::sum);
                items.putIfAbsent(id, item);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> items.get(e.getKey()))
                .collect(Collectors.toList());
    }
}
