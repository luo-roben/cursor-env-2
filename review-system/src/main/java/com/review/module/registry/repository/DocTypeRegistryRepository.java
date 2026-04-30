package com.review.module.registry.repository;

import com.review.module.registry.entity.DocTypeRegistryDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocTypeRegistryRepository extends JpaRepository<DocTypeRegistryDO, Long> {

    Optional<DocTypeRegistryDO> findByDocumentType(String documentType);

    List<DocTypeRegistryDO> findByEnabledTrue();
}
