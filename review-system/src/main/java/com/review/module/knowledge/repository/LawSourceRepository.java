package com.review.module.knowledge.repository;

import com.review.module.knowledge.entity.LawSourceDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawSourceRepository extends JpaRepository<LawSourceDO, Long> {

    Optional<LawSourceDO> findBySourceId(String sourceId);

    List<LawSourceDO> findByParseStatus(String parseStatus);

    @Query("SELECT ls FROM LawSourceDO ls WHERE "
            + "(:title IS NULL OR ls.title LIKE CONCAT('%', :title, '%')) "
            + "AND (:docType IS NULL OR ls.docType = :docType) "
            + "AND (:status IS NULL OR ls.status = :status) "
            + "AND (:parseStatus IS NULL OR ls.parseStatus = :parseStatus)")
    Page<LawSourceDO> findByFilters(
            @Param("title") String title,
            @Param("docType") String docType,
            @Param("status") String status,
            @Param("parseStatus") String parseStatus,
            Pageable pageable);
}
