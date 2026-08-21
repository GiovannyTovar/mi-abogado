package com.miabogado.domain.settlement.repository;

import com.miabogado.domain.settlement.dto.SettlementSummary;
import com.miabogado.domain.settlement.entity.SettlementCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SettlementCalculationRepository extends JpaRepository<SettlementCalculation, UUID> {

    @EntityGraph(attributePaths = {"client", "legalCase", "createdBy"})
    Optional<SettlementCalculation> findWithDetailsById(UUID id);

    @Query(value = """
            select new com.miabogado.domain.settlement.dto.SettlementSummary(
                s.id, s.employeeName, c.id, c.name, lc.id, lc.caseNumber,
                s.endDate, s.total, s.createdAt)
            from SettlementCalculation s
              left join s.client c
              left join s.legalCase lc
            where (:clientId is null or c.id = :clientId)
              and (:caseId is null or lc.id = :caseId)
            """,
            countQuery = """
            select count(s) from SettlementCalculation s
              left join s.client c
              left join s.legalCase lc
            where (:clientId is null or c.id = :clientId)
              and (:caseId is null or lc.id = :caseId)
            """)
    Page<SettlementSummary> search(@Param("clientId") UUID clientId,
                                   @Param("caseId") UUID caseId,
                                   Pageable pageable);
}
