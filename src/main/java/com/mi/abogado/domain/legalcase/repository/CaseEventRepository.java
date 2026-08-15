package com.mi.abogado.domain.legalcase.repository;

import com.mi.abogado.domain.legalcase.dto.CaseEventResponse;
import com.mi.abogado.domain.legalcase.entity.CaseEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CaseEventRepository extends JpaRepository<CaseEvent, UUID> {

    /** Bitacora del expediente, de lo mas reciente a lo mas antiguo. */
    @Query(value = """
            select new com.mi.abogado.domain.legalcase.dto.CaseEventResponse(
                e.id, e.eventType, e.title, e.description, u.fullName, e.occurredAt)
            from CaseEvent e
              left join e.createdBy u
            where e.legalCase.id = :caseId
            order by e.occurredAt desc
            """,
            countQuery = "select count(e) from CaseEvent e where e.legalCase.id = :caseId")
    Page<CaseEventResponse> findByCaseId(@Param("caseId") UUID caseId, Pageable pageable);
}
