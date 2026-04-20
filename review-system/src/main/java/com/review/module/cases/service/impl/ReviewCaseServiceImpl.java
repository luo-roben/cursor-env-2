package com.review.module.cases.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.PageResult;
import com.review.module.cases.entity.ReviewCaseDO;
import com.review.module.cases.repository.ReviewCaseRepository;
import com.review.module.cases.service.ReviewCaseService;
import com.review.module.cases.vo.CaseCreateReqVO;
import com.review.module.cases.vo.CasePageReqVO;
import com.review.module.cases.vo.CaseRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewCaseServiceImpl implements ReviewCaseService {

    private final ReviewCaseRepository reviewCaseRepository;

    @Override
    @Transactional
    public CaseRespVO create(CaseCreateReqVO reqVO) {
        ReviewCaseDO entity = ReviewCaseDO.builder()
                .tenantId(reqVO.getTenantId())
                .source(reqVO.getSource())
                .documentType(reqVO.getDocumentType())
                .contentType(reqVO.getContentType())
                .contractType(reqVO.getContractType())
                .productType(reqVO.getProductType())
                .channel(reqVO.getChannel())
                .reviewCard(reqVO.getReviewCard())
                .reviewedContent(reqVO.getReviewedContent())
                .verdict(reqVO.getVerdict())
                .severity(reqVO.getSeverity())
                .reason(reqVO.getReason())
                .lawReferences(reqVO.getLawReferences())
                .suggestion(reqVO.getSuggestion())
                .suggestionType(reqVO.getSuggestionType())
                .aiOriginalVerdict(reqVO.getAiOriginalVerdict())
                .humanAction(reqVO.getHumanAction())
                .humanReviewerId(reqVO.getHumanReviewerId())
                .isTypical(reqVO.getIsTypical() != null ? reqVO.getIsTypical() : false)
                .build();

        entity = reviewCaseRepository.save(entity);
        log.info("Case created: id={}, tenantId={}", entity.getId(), entity.getTenantId());
        return toRespVO(entity);
    }

    @Override
    public CaseRespVO getById(Long id) {
        ReviewCaseDO entity = reviewCaseRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.CASE_NOT_FOUND));
        return toRespVO(entity);
    }

    @Override
    public PageResult<CaseRespVO> page(CasePageReqVO reqVO) {
        PageRequest pageRequest = PageRequest.of(
                reqVO.getPageNum() - 1, reqVO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReviewCaseDO> page;
        if (reqVO.getTenantId() != null) {
            page = reviewCaseRepository.findByTenantId(reqVO.getTenantId(), pageRequest);
        } else {
            page = reviewCaseRepository.findAll(pageRequest);
        }

        List<CaseRespVO> list = page.getContent().stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());

        return PageResult.of(list, page.getTotalElements(), reqVO.getPageNum(), reqVO.getPageSize());
    }

    @Override
    public List<CaseRespVO> listByContentType(String contentType) {
        return reviewCaseRepository.findByContentTypeAndStatus(contentType, "active").stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CaseRespVO> listTypicalCases() {
        return reviewCaseRepository.findByIsTypicalTrue().stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
    }

    private CaseRespVO toRespVO(ReviewCaseDO entity) {
        return CaseRespVO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .source(entity.getSource())
                .documentType(entity.getDocumentType())
                .contentType(entity.getContentType())
                .contractType(entity.getContractType())
                .productType(entity.getProductType())
                .channel(entity.getChannel())
                .reviewCard(entity.getReviewCard())
                .reviewedContent(entity.getReviewedContent())
                .verdict(entity.getVerdict())
                .severity(entity.getSeverity())
                .reason(entity.getReason())
                .lawReferences(entity.getLawReferences())
                .suggestion(entity.getSuggestion())
                .suggestionType(entity.getSuggestionType())
                .aiOriginalVerdict(entity.getAiOriginalVerdict())
                .humanAction(entity.getHumanAction())
                .humanReviewerId(entity.getHumanReviewerId())
                .learningValueScore(entity.getLearningValueScore())
                .isTypical(entity.getIsTypical())
                .isShared(entity.getIsShared())
                .decayWeight(entity.getDecayWeight())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
