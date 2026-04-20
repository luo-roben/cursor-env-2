package com.review.module.cases.service;

public interface CaseSedimentationService {

    int calculateLearningValue(String humanAction);

    void sediment(Long feedbackId);
}
