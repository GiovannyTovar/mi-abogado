package com.mi.abogado.domain.appointment.repository;

import com.mi.abogado.domain.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @EntityGraph(attributePaths = {"client", "legalCase", "lawyer", "lawyer.user"})
    Optional<Appointment> findWithDetailsById(UUID id);

    /** Agenda de la firma en una ventana de fechas. */
    @EntityGraph(attributePaths = {"client", "legalCase", "lawyer", "lawyer.user"})
    @Query("""
            select a from Appointment a
            where a.startsAt >= :from and a.startsAt < :to
              and (:lawyerId is null or a.lawyer.id = :lawyerId)
              and (:clientId is null or a.client.id = :clientId)
            order by a.startsAt asc
            """)
    List<Appointment> findAgenda(@Param("from") Instant from,
                                 @Param("to") Instant to,
                                 @Param("lawyerId") UUID lawyerId,
                                 @Param("clientId") UUID clientId);

    /** Proximas citas del cliente en su portal. */
    @EntityGraph(attributePaths = {"client", "legalCase", "lawyer", "lawyer.user"})
    @Query("""
            select a from Appointment a
            where a.client.id = :clientId and a.startsAt >= :from
            order by a.startsAt asc
            """)
    List<Appointment> findUpcomingForClient(@Param("clientId") UUID clientId, @Param("from") Instant from);

    /**
     * Choques de agenda del abogado. Dos citas se solapan si cada una empieza
     * antes de que termine la otra.
     */
    @Query("""
            select count(a) from Appointment a
            where a.lawyer.id = :lawyerId
              and a.status in (com.mi.abogado.domain.appointment.entity.AppointmentStatus.SCHEDULED,
                               com.mi.abogado.domain.appointment.entity.AppointmentStatus.CONFIRMED)
              and a.startsAt < :endsAt and a.endsAt > :startsAt
              and (:excludeId is null or a.id <> :excludeId)
            """)
    long countOverlapping(@Param("lawyerId") UUID lawyerId,
                          @Param("startsAt") Instant startsAt,
                          @Param("endsAt") Instant endsAt,
                          @Param("excludeId") UUID excludeId);
}
