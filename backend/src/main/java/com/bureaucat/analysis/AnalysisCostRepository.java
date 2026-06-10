package com.bureaucat.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisCostRepository extends JpaRepository<AnalysisCost, UUID> {

    List<AnalysisCost> findByCardId(UUID cardId);
}
