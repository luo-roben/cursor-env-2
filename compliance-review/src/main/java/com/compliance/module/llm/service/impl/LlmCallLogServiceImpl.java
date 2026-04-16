package com.compliance.module.llm.service.impl;

import com.compliance.module.llm.entity.LlmCallLogDO;
import com.compliance.module.llm.repository.LlmCallLogRepository;
import com.compliance.module.llm.service.LlmCallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmCallLogServiceImpl implements LlmCallLogService {

    private final LlmCallLogRepository llmCallLogRepository;

    @Override
    @Transactional
    public LlmCallLogDO log(LlmCallLogDO logEntry) {
        logEntry = llmCallLogRepository.save(logEntry);
        log.info("Logged LLM call: id={}, callType={}, model={}, success={}",
                logEntry.getId(), logEntry.getCallType(), logEntry.getModelName(), logEntry.getSuccess());
        return logEntry;
    }

    @Override
    public List<LlmCallLogDO> listByTaskId(Long reviewTaskId) {
        return llmCallLogRepository.findByReviewTaskId(reviewTaskId);
    }
}
