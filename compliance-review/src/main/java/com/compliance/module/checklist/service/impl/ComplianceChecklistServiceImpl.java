package com.compliance.module.checklist.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.module.checklist.entity.ComplianceChecklistDO;
import com.compliance.module.checklist.repository.ComplianceChecklistRepository;
import com.compliance.module.checklist.service.ComplianceChecklistService;
import com.compliance.module.checklist.vo.ChecklistCreateReqVO;
import com.compliance.module.checklist.vo.ChecklistRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceChecklistServiceImpl implements ComplianceChecklistService {

    private final ComplianceChecklistRepository checklistRepository;

    @Override
    @Transactional
    public ChecklistRespVO create(ChecklistCreateReqVO reqVO) {
        ComplianceChecklistDO entity = ComplianceChecklistDO.builder()
                .contentType(reqVO.getContentType())
                .productType(reqVO.getProductType())
                .checkItem(reqVO.getCheckItem())
                .checkMethod(reqVO.getCheckMethod())
                .lawArticleId(reqVO.getLawArticleId())
                .conditionDesc(reqVO.getConditionDesc())
                .severityIfMissing(reqVO.getSeverityIfMissing() != null ? reqVO.getSeverityIfMissing() : "major")
                .tenantId(reqVO.getTenantId())
                .enabled(true)
                .build();

        entity = checklistRepository.save(entity);
        log.info("Created checklist item: id={}, checkItem={}", entity.getId(), entity.getCheckItem());
        return toRespVO(entity);
    }

    @Override
    public List<ChecklistRespVO> listByContentType(String contentType, String productType) {
        List<ComplianceChecklistDO> list;
        if (productType != null && !productType.isBlank()) {
            list = checklistRepository.findByContentTypeAndProductTypeAndEnabledTrue(contentType, productType);
        } else {
            list = checklistRepository.findByContentTypeAndEnabledTrue(contentType);
        }
        return list.stream().map(this::toRespVO).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ComplianceChecklistDO entity = checklistRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
        checklistRepository.delete(entity);
        log.info("Deleted checklist item: id={}", id);
    }

    private ChecklistRespVO toRespVO(ComplianceChecklistDO entity) {
        ChecklistRespVO vo = new ChecklistRespVO();
        vo.setId(entity.getId());
        vo.setContentType(entity.getContentType());
        vo.setProductType(entity.getProductType());
        vo.setCheckItem(entity.getCheckItem());
        vo.setCheckMethod(entity.getCheckMethod());
        vo.setLawArticleId(entity.getLawArticleId());
        vo.setConditionDesc(entity.getConditionDesc());
        vo.setSeverityIfMissing(entity.getSeverityIfMissing());
        vo.setTenantId(entity.getTenantId());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
