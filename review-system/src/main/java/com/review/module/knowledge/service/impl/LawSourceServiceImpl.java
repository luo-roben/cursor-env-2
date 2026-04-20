package com.review.module.knowledge.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.PageResult;
import com.review.module.knowledge.entity.LawSourceDO;
import com.review.module.knowledge.repository.LawSourceRepository;
import com.review.module.knowledge.service.LawSourceService;
import com.review.module.knowledge.vo.LawSourceCreateReqVO;
import com.review.module.knowledge.vo.LawSourcePageReqVO;
import com.review.module.knowledge.vo.LawSourceRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LawSourceServiceImpl implements LawSourceService {

    private final LawSourceRepository lawSourceRepository;

    @Override
    @Transactional
    public LawSourceRespVO create(LawSourceCreateReqVO reqVO) {
        LawSourceDO entity = LawSourceDO.builder()
                .sourceId(reqVO.getSourceId())
                .title(reqVO.getTitle())
                .issuer(reqVO.getIssuer())
                .issueDate(reqVO.getIssueDate())
                .effectiveDate(reqVO.getEffectiveDate())
                .docType(reqVO.getDocType())
                .status(reqVO.getStatus())
                .fullText(reqVO.getFullText())
                .sourceUrl(reqVO.getSourceUrl())
                .applicableDocTypes(reqVO.getApplicableDocTypes())
                .build();
        LawSourceDO saved = lawSourceRepository.save(entity);
        return toRespVO(saved);
    }

    @Override
    public LawSourceRespVO getById(Long id) {
        LawSourceDO entity = lawSourceRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_SOURCE_NOT_FOUND));
        return toRespVO(entity);
    }

    @Override
    public PageResult<LawSourceRespVO> page(LawSourcePageReqVO reqVO) {
        PageRequest pageRequest = PageRequest.of(
                reqVO.getPageNum() - 1,
                reqVO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LawSourceDO> page = lawSourceRepository.findByFilters(
                reqVO.getTitle(),
                reqVO.getDocType(),
                reqVO.getStatus(),
                reqVO.getParseStatus(),
                pageRequest);
        return PageResult.of(
                page.getContent().stream().map(this::toRespVO).toList(),
                page.getTotalElements(),
                reqVO.getPageNum(),
                reqVO.getPageSize());
    }

    @Override
    @Transactional
    public void updateParseStatus(Long id, String parseStatus) {
        LawSourceDO entity = lawSourceRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_SOURCE_NOT_FOUND));
        entity.setParseStatus(parseStatus);
        lawSourceRepository.save(entity);
    }

    private LawSourceRespVO toRespVO(LawSourceDO entity) {
        return LawSourceRespVO.builder()
                .id(entity.getId())
                .sourceId(entity.getSourceId())
                .title(entity.getTitle())
                .issuer(entity.getIssuer())
                .issueDate(entity.getIssueDate())
                .effectiveDate(entity.getEffectiveDate())
                .docType(entity.getDocType())
                .status(entity.getStatus())
                .fullText(entity.getFullText())
                .sourceUrl(entity.getSourceUrl())
                .applicableDocTypes(entity.getApplicableDocTypes())
                .parseStatus(entity.getParseStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
