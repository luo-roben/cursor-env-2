package com.compliance.module.law.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.common.result.PageResult;
import com.compliance.module.law.entity.LawSourceDO;
import com.compliance.module.law.repository.LawSourceRepository;
import com.compliance.module.law.service.LawSourceService;
import com.compliance.module.law.vo.LawSourceCreateReqVO;
import com.compliance.module.law.vo.LawSourcePageReqVO;
import com.compliance.module.law.vo.LawSourceRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
                .fullText(reqVO.getFullText())
                .sourceUrl(reqVO.getSourceUrl())
                .build();

        entity = lawSourceRepository.save(entity);
        log.info("Created law source: id={}, sourceId={}", entity.getId(), entity.getSourceId());
        return toRespVO(entity);
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

        List<LawSourceRespVO> list = page.getContent().stream()
                .map(this::toRespVO)
                .toList();

        return PageResult.of(list, page.getTotalElements(), reqVO.getPageNum(), reqVO.getPageSize());
    }

    @Override
    @Transactional
    public void updateParseStatus(Long id, String parseStatus) {
        LawSourceDO entity = lawSourceRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_SOURCE_NOT_FOUND));
        entity.setParseStatus(parseStatus);
        lawSourceRepository.save(entity);
        log.info("Updated law source parse status: id={}, parseStatus={}", id, parseStatus);
    }

    private LawSourceRespVO toRespVO(LawSourceDO entity) {
        LawSourceRespVO vo = new LawSourceRespVO();
        vo.setId(entity.getId());
        vo.setSourceId(entity.getSourceId());
        vo.setTitle(entity.getTitle());
        vo.setIssuer(entity.getIssuer());
        vo.setIssueDate(entity.getIssueDate());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setDocType(entity.getDocType());
        vo.setStatus(entity.getStatus());
        vo.setFullText(entity.getFullText());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setParseStatus(entity.getParseStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
