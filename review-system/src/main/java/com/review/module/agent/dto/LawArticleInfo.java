package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LawArticleInfo {

    private Long id;
    private String lawName;
    private String articleId;
    private String originalText;
    private String normType;
    private List<String> keyPhrases;
}
