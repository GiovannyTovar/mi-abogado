package com.miabogado.domain.lead.repository;

import com.miabogado.domain.lead.dto.LeadPipelineStage;
import com.miabogado.domain.lead.dto.LeadSummary;
import com.miabogado.domain.lead.entity.Lead;
import com.miabogado.domain.lead.entity.LeadSource;
import com.miabogado.domain.lead.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    @EntityGraph(attributePaths = {"practiceArea", "assignedLawyer", "assignedLawyer.user", "convertedClient"})
    Optional<Lead> findWithDetailsById(UUID id);

    @Query(value = """
            select new com.miabogado.domain.lead.dto.LeadSummary(
                le.id, le.name, le.email, le.phone, le.city, le.source, le.status,
                pa.name, u.fullName, le.createdAt)
            from Lead le
              left join le.practiceArea pa
              left join le.assignedLawyer l
              left join l.user u
            where (:status is null or le.status = :status)
              and (:source is null or le.source = :source)
              and (:lawyerId is null or l.id = :lawyerId)
            """,
            countQuery = """
            select count(le) from Lead le
              left join le.assignedLawyer l
            where (:status is null or le.status = :status)
              and (:source is null or le.source = :source)
              and (:lawyerId is null or l.id = :lawyerId)
            """)
    Page<LeadSummary> search(@Param("status") LeadStatus status,
                             @Param("source") LeadSource source,
                             @Param("lawyerId") UUID lawyerId,
                             Pageable pageable);

    /** Tablero: un conteo por etapa en una sola consulta. */
    @Query("""
            select new com.miabogado.domain.lead.dto.LeadPipelineStage(le.status, count(le))
            from Lead le
            group by le.status
            """)
    List<LeadPipelineStage> countByStage();
}
