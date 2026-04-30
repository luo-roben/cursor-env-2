package com.review.module.knowledge.repository;

import com.review.module.knowledge.entity.LawNameAliasDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LawNameAliasRepository extends JpaRepository<LawNameAliasDO, Long> {

    List<LawNameAliasDO> findByAlias(String alias);

    List<LawNameAliasDO> findByLawName(String lawName);
}
