package com.compliance.module.cases.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.common.result.PageResult;
import com.compliance.module.cases.entity.ReviewCaseDO;
import com.compliance.module.cases.repository.ReviewCaseRepository;
import com.compliance.module.cases.service.ReviewCaseService;
import com.compliance.module.cases.vo.CaseCreateReqVO;
import com.compliance.module.cases.vo.CasePageReqVO;
import com.compliance.module.cases.vo.CaseRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .source(reqVO.getSource() != null ? reqVO.getSource() : "manual_import")
                .contentType(reqVO.getContentType())
                .productType(reqVO.getProductType())
                .channel(reqVO.getChannel())
                .reviewedContent(reqVO.getReviewedContent())
                .verdict(reqVO.getVerdict())
                .severity(reqVO.getSeverity())
                .reason(reqVO.getReason())
                .lawReferences(reqVO.getLawReferences())
                .suggestion(reqVO.getSuggestion())
                .build();

        entity = reviewCaseRepository.save(entity);
        log.info("Created review case: id={}, source={}", entity.getId(), entity.getSource());
        return toRespVO(entity);
    }

    @Override
    public PageResult<CaseRespVO> page(CasePageReqVO reqVO) {
        PageRequest pageRequest = PageRequest.of(reqVO.getPageNum() - 1, reqVO.getPageSize());

        Page<ReviewCaseDO> page = reviewCaseRepository.findByFilters(
                reqVO.getTenantId(),
                reqVO.getVerdict(),
                reqVO.getContentType(),
                reqVO.getProductType(),
                pageRequest);

        List<CaseRespVO> list = page.getContent().stream()
                .map(this::toRespVO)
                .toList();

        return PageResult.of(list, page.getTotalElements(), reqVO.getPageNum(), reqVO.getPageSize());
    }

    @Override
    public CaseRespVO getById(Long id, Long tenantId) {
        ReviewCaseDO entity = reviewCaseRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.CASE_NOT_FOUND));

        if (!entity.getTenantId().equals(tenantId)) {
            throw new ServiceException(ErrorCode.CASE_NOT_FOUND);
        }

        return toRespVO(entity);
    }

    private CaseRespVO toRespVO(ReviewCaseDO entity) {
        CaseRespVO vo = new CaseRespVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setSource(entity.getSource());
        vo.setContentType(entity.getContentType());
        vo.setProductType(entity.getProductType());
        vo.setChannel(entity.getChannel());
        vo.setReviewedContent(entity.getReviewedContent());
        vo.setVerdict(entity.getVerdict());
        vo.setSeverity(entity.getSeverity());
        vo.setReason(entity.getReason());
        vo.setLawReferences(entity.getLawReferences());
        vo.setSuggestion(entity.getSuggestion());
        vo.setAiOriginalVerdict(entity.getAiOriginalVerdict());
        vo.setHumanAction(entity.getHumanAction());
        vo.setHumanReviewerId(entity.getHumanReviewerId());
        vo.setLearningValueScore(entity.getLearningValueScore());
        vo.setIsTypical(entity.getIsTypical());
        vo.setIsShared(entity.getIsShared());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
