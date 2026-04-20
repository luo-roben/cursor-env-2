package com.review.module.knowledge.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.PageResult;
import com.review.module.knowledge.entity.ContractTemplateClauseDO;
import com.review.module.knowledge.entity.ContractTemplateDO;
import com.review.module.knowledge.repository.ContractTemplateClauseRepository;
import com.review.module.knowledge.repository.ContractTemplateRepository;
import com.review.module.knowledge.service.ContractTemplateService;
import com.review.module.knowledge.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateClauseRepository contractTemplateClauseRepository;

    @Override
    @Transactional
    public ContractTemplateRespVO create(ContractTemplateCreateReqVO reqVO) {
        ContractTemplateDO entity = ContractTemplateDO.builder()
                .templateId(reqVO.getTemplateId())
                .templateName(reqVO.getTemplateName())
                .contractType(reqVO.getContractType())
                .industry(reqVO.getIndustry())
                .source(reqVO.getSource())
                .sourceName(reqVO.getSourceName())
                .version(reqVO.getVersion())
                .tenantId(reqVO.getTenantId())
                .totalClauses(reqVO.getTotalClauses())
                .totalChars(reqVO.getTotalChars())
                .parties(reqVO.getParties())
                .requiredClauseRoles(reqVO.getRequiredClauseRoles())
                .applicableScenarios(reqVO.getApplicableScenarios())
                .build();
        ContractTemplateDO saved = contractTemplateRepository.save(entity);
        return toRespVO(saved);
    }

    @Override
    public ContractTemplateRespVO getById(Long id) {
        ContractTemplateDO entity = contractTemplateRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TEMPLATE_NOT_FOUND));
        return toRespVO(entity);
    }

    @Override
    public PageResult<ContractTemplateRespVO> page(String templateName, String contractType, String status,
                                                    Long tenantId, Integer pageNum, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(
                pageNum - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ContractTemplateDO> page = contractTemplateRepository.findByFilters(
                templateName, contractType, status, tenantId, pageRequest);
        return PageResult.of(
                page.getContent().stream().map(this::toRespVO).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    @Override
    @Transactional
    public ContractTemplateClauseRespVO addClause(Long templateId, ContractTemplateClauseCreateReqVO reqVO) {
        ContractTemplateDO template = contractTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ServiceException(ErrorCode.TEMPLATE_NOT_FOUND));
        ContractTemplateClauseDO clause = ContractTemplateClauseDO.builder()
                .templateId(template.getTemplateId())
                .clauseId(reqVO.getClauseId())
                .clauseTitle(reqVO.getClauseTitle())
                .clauseRole(reqVO.getClauseRole())
                .clauseLevel(reqVO.getClauseLevel())
                .parentClauseId(reqVO.getParentClauseId())
                .clauseText(reqVO.getClauseText())
                .isRequired(reqVO.getIsRequired())
                .riskLevel(reqVO.getRiskLevel())
                .annotations(reqVO.getAnnotations())
                .charOffsetStart(reqVO.getCharOffsetStart())
                .charOffsetEnd(reqVO.getCharOffsetEnd())
                .build();
        ContractTemplateClauseDO saved = contractTemplateClauseRepository.save(clause);
        return toClauseRespVO(saved);
    }

    @Override
    public List<ContractTemplateClauseRespVO> listClauses(Long templateId) {
        ContractTemplateDO template = contractTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ServiceException(ErrorCode.TEMPLATE_NOT_FOUND));
        List<ContractTemplateClauseDO> clauses = contractTemplateClauseRepository
                .findByTemplateIdOrderByClauseIdAsc(template.getTemplateId());
        return clauses.stream().map(this::toClauseRespVO).toList();
    }

    @Override
    @Transactional
    public void publish(Long id) {
        ContractTemplateDO entity = contractTemplateRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TEMPLATE_NOT_FOUND));
        entity.setStatus("published");
        contractTemplateRepository.save(entity);
    }

    private ContractTemplateRespVO toRespVO(ContractTemplateDO entity) {
        return ContractTemplateRespVO.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .templateName(entity.getTemplateName())
                .contractType(entity.getContractType())
                .industry(entity.getIndustry())
                .source(entity.getSource())
                .sourceName(entity.getSourceName())
                .version(entity.getVersion())
                .status(entity.getStatus())
                .tenantId(entity.getTenantId())
                .totalClauses(entity.getTotalClauses())
                .totalChars(entity.getTotalChars())
                .parties(entity.getParties())
                .requiredClauseRoles(entity.getRequiredClauseRoles())
                .applicableScenarios(entity.getApplicableScenarios())
                .confirmedBy(entity.getConfirmedBy())
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ContractTemplateClauseRespVO toClauseRespVO(ContractTemplateClauseDO entity) {
        return ContractTemplateClauseRespVO.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .clauseId(entity.getClauseId())
                .clauseTitle(entity.getClauseTitle())
                .clauseRole(entity.getClauseRole())
                .clauseLevel(entity.getClauseLevel())
                .parentClauseId(entity.getParentClauseId())
                .clauseText(entity.getClauseText())
                .isRequired(entity.getIsRequired())
                .riskLevel(entity.getRiskLevel())
                .annotations(entity.getAnnotations())
                .charOffsetStart(entity.getCharOffsetStart())
                .charOffsetEnd(entity.getCharOffsetEnd())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
