package com.miabogado.domain.legalcase.repository;

import com.miabogado.domain.legalcase.dto.UpcomingDeadline;
import com.miabogado.domain.legalcase.entity.CaseDeadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CaseDeadlineRepository extends JpaRepository<CaseDeadline, UUID> {

    List<CaseDeadline> findByLegalCase_IdOrderByDueDateAsc(UUID caseId);

    /**
     * Agenda de la firma: lo que vence de aqui a {@code until}, incluido lo ya
     * vencido y aun pendiente (que es justo lo urgente).
     */
    @Query("""
            select new com.miabogado.domain.legalcase.dto.UpcomingDeadline(
                d.id, lc.id, lc.caseNumber, lc.title, c.name, u.fullName,
                d.deadlineType, d.title, d.dueDate, d.notifyDaysBefore)
            from CaseDeadline d
              join d.legalCase lc
              join lc.client c
              left join lc.assignedLawyer l
              left join l.user u
            where d.status = com.miabogado.domain.legalcase.entity.DeadlineStatus.PENDING
              and d.dueDate <= :until
              and (:lawyerId is null or l.id = :lawyerId)
            order by d.dueDate asc
            """)
    List<UpcomingDeadline> findUpcoming(@Param("until") LocalDate until,
                                        @Param("lawyerId") UUID lawyerId);

    /**
     * Vencidos sin cumplir. Consulta del job diario: cruza todas las firmas, por
     * eso es nativa — el filtro de tenant de Hibernate la limitaria a una sola.
     */
    @Modifying
    @Query(value = """
            update case_deadline
               set status = 'MISSED', updated_at = now()
             where status = 'PENDING' and due_date < :today
            """, nativeQuery = true)
    int markOverdueAsMissed(@Param("today") LocalDate today);
}
