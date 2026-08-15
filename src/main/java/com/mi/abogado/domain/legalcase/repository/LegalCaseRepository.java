package com.mi.abogado.domain.legalcase.repository;

import com.mi.abogado.domain.legalcase.dto.CaseSummary;
import com.mi.abogado.domain.legalcase.entity.CaseStatus;
import com.mi.abogado.domain.legalcase.entity.CaseType;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {

    /** Detalle: cliente, abogado (con su usuario) y especialidad en una sola consulta. */
    @EntityGraph(attributePaths = {"client", "assignedLawyer", "assignedLawyer.user", "practiceArea"})
    Optional<LegalCase> findWithDetailsById(UUID id);

    boolean existsByRadicado(String radicado);

    /**
     * Listado de expedientes. La subconsulta del proximo vencimiento va dentro
     * de la misma proyeccion: sin ella harian falta N consultas extra para pintar
     * la columna que mas se mira.
     */
    @Query(value = """
            select new com.mi.abogado.domain.legalcase.dto.CaseSummary(
                lc.id, lc.caseNumber, lc.title, lc.caseType, lc.status, lc.priority,
                c.name, u.fullName,
                (select min(d.dueDate) from CaseDeadline d
                  where d.legalCase = lc and d.status = com.mi.abogado.domain.legalcase.entity.DeadlineStatus.PENDING),
                lc.openedAt)
            from LegalCase lc
              join lc.client c
              left join lc.assignedLawyer l
              left join l.user u
            where (:status is null or lc.status = :status)
              and (:caseType is null or lc.caseType = :caseType)
              and (:clientId is null or c.id = :clientId)
              and (:lawyerId is null or l.id = :lawyerId)
              and (:search is null
                   or lower(lc.title) like lower(concat('%', :search, '%'))
                   or lower(lc.caseNumber) like lower(concat('%', :search, '%'))
                   or lc.radicado like concat('%', :search, '%'))
            """,
            countQuery = """
            select count(lc)
            from LegalCase lc
              join lc.client c
              left join lc.assignedLawyer l
            where (:status is null or lc.status = :status)
              and (:caseType is null or lc.caseType = :caseType)
              and (:clientId is null or c.id = :clientId)
              and (:lawyerId is null or l.id = :lawyerId)
              and (:search is null
                   or lower(lc.title) like lower(concat('%', :search, '%'))
                   or lower(lc.caseNumber) like lower(concat('%', :search, '%'))
                   or lc.radicado like concat('%', :search, '%'))
            """)
    Page<CaseSummary> search(@Param("status") CaseStatus status,
                             @Param("caseType") CaseType caseType,
                             @Param("clientId") UUID clientId,
                             @Param("lawyerId") UUID lawyerId,
                             @Param("search") String search,
                             Pageable pageable);
}
