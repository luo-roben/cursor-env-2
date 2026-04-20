package com.review.module.knowledge.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.PageResult;
import com.review.module.knowledge.entity.LawArticleDO;
import com.review.module.knowledge.repository.LawArticleRepository;
import com.review.module.knowledge.service.LawArticleService;
import com.review.module.knowledge.vo.LawArticleCreateReqVO;
import com.review.module.knowledge.vo.LawArticlePageReqVO;
import com.review.module.knowledge.vo.LawArticleRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LawArticleServiceImpl implements LawArticleService {

    private final LawArticleRepository lawArticleRepository;

    @Override
    @Transactional
    public LawArticleRespVO create(LawArticleCreateReqVO reqVO) {
        LawArticleDO entity = LawArticleDO.builder()
                .sourceId(reqVO.getSourceId())
                .lawName(reqVO.getLawName())
                .lawShortName(reqVO.getLawShortName())
                .articleId(reqVO.getArticleId())
                .originalText(reqVO.getOriginalText())
                .normType(reqVO.getNormType())
                .subject(reqVO.getSubject())
                .behavior(reqVO.getBehavior())
                .objectDesc(reqVO.getObjectDesc())
                .applicableCondition(reqVO.getApplicableCondition())
                .applicableScenarios(reqVO.getApplicableScenarios())
                .applicableContentTypes(reqVO.getApplicableContentTypes())
                .applicableProductTypes(reqVO.getApplicableProductTypes())
                .applicableDocTypes(reqVO.getApplicableDocTypes())
                .contractClauseRole(reqVO.getContractClauseRole())
                .contractTypes(reqVO.getContractTypes())
                .keyPhrases(reqVO.getKeyPhrases())
                .semanticExtensions(reqVO.getSemanticExtensions())
                .violationExamples(reqVO.getViolationExamples())
                .compliantExamples(reqVO.getCompliantExamples())
                .penalty(reqVO.getPenalty())
                .relatedArticles(reqVO.getRelatedArticles())
                .authorityLevel(reqVO.getAuthorityLevel())
                .build();
        LawArticleDO saved = lawArticleRepository.save(entity);
        return toRespVO(saved);
    }

    @Override
    public LawArticleRespVO getById(Long id) {
        LawArticleDO entity = lawArticleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND));
        return toRespVO(entity);
    }

    @Override
    public PageResult<LawArticleRespVO> page(LawArticlePageReqVO reqVO) {
        PageRequest pageRequest = PageRequest.of(
                reqVO.getPageNum() - 1,
                reqVO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LawArticleDO> page = lawArticleRepository.findByFilters(
                reqVO.getLawName(),
                reqVO.getArticleId(),
                reqVO.getNormType(),
                reqVO.getStatus(),
                reqVO.getAuthorityLevel(),
                pageRequest);
        return PageResult.of(
                page.getContent().stream().map(this::toRespVO).toList(),
                page.getTotalElements(),
                reqVO.getPageNum(),
                reqVO.getPageSize());
    }

    @Override
    @Transactional
    public void publish(Long id) {
        LawArticleDO entity = lawArticleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND));
        if ("published".equals(entity.getStatus())) {
            throw new ServiceException(ErrorCode.LAW_ARTICLE_ALREADY_PUBLISHED);
        }
        entity.setStatus("published");
        lawArticleRepository.save(entity);
    }

    @Override
    @Transactional
    public LawArticleRespVO update(Long id, LawArticleCreateReqVO reqVO) {
        LawArticleDO entity = lawArticleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND));
        entity.setSourceId(reqVO.getSourceId());
        entity.setLawName(reqVO.getLawName());
        entity.setLawShortName(reqVO.getLawShortName());
        entity.setArticleId(reqVO.getArticleId());
        entity.setOriginalText(reqVO.getOriginalText());
        entity.setNormType(reqVO.getNormType());
        entity.setSubject(reqVO.getSubject());
        entity.setBehavior(reqVO.getBehavior());
        entity.setObjectDesc(reqVO.getObjectDesc());
        entity.setApplicableCondition(reqVO.getApplicableCondition());
        entity.setApplicableScenarios(reqVO.getApplicableScenarios());
        entity.setApplicableContentTypes(reqVO.getApplicableContentTypes());
        entity.setApplicableProductTypes(reqVO.getApplicableProductTypes());
        entity.setApplicableDocTypes(reqVO.getApplicableDocTypes());
        entity.setContractClauseRole(reqVO.getContractClauseRole());
        entity.setContractTypes(reqVO.getContractTypes());
        entity.setKeyPhrases(reqVO.getKeyPhrases());
        entity.setSemanticExtensions(reqVO.getSemanticExtensions());
        entity.setViolationExamples(reqVO.getViolationExamples());
        entity.setCompliantExamples(reqVO.getCompliantExamples());
        entity.setPenalty(reqVO.getPenalty());
        entity.setRelatedArticles(reqVO.getRelatedArticles());
        if (reqVO.getAuthorityLevel() != null) {
            entity.setAuthorityLevel(reqVO.getAuthorityLevel());
        }
        LawArticleDO saved = lawArticleRepository.save(entity);
        return toRespVO(saved);
    }

    @Override
    @Transactional
    public void deprecate(Long id) {
        LawArticleDO entity = lawArticleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND));
        entity.setStatus("deprecated");
        lawArticleRepository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!lawArticleRepository.existsById(id)) {
            throw new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND);
        }
        lawArticleRepository.deleteById(id);
    }

    private LawArticleRespVO toRespVO(LawArticleDO entity) {
        return LawArticleRespVO.builder()
                .id(entity.getId())
                .sourceId(entity.getSourceId())
                .lawName(entity.getLawName())
                .lawShortName(entity.getLawShortName())
                .articleId(entity.getArticleId())
                .originalText(entity.getOriginalText())
                .normType(entity.getNormType())
                .subject(entity.getSubject())
                .behavior(entity.getBehavior())
                .objectDesc(entity.getObjectDesc())
                .applicableCondition(entity.getApplicableCondition())
                .applicableScenarios(entity.getApplicableScenarios())
                .applicableContentTypes(entity.getApplicableContentTypes())
                .applicableProductTypes(entity.getApplicableProductTypes())
                .applicableDocTypes(entity.getApplicableDocTypes())
                .contractClauseRole(entity.getContractClauseRole())
                .contractTypes(entity.getContractTypes())
                .keyPhrases(entity.getKeyPhrases())
                .semanticExtensions(entity.getSemanticExtensions())
                .violationExamples(entity.getViolationExamples())
                .compliantExamples(entity.getCompliantExamples())
                .penalty(entity.getPenalty())
                .relatedArticles(entity.getRelatedArticles())
                .authorityLevel(entity.getAuthorityLevel())
                .status(entity.getStatus())
                .confirmedBy(entity.getConfirmedBy())
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
