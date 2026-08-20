package com.miabogado.domain.message.repository;

import com.miabogado.domain.message.dto.MessageResponse;
import com.miabogado.domain.message.entity.CaseMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface CaseMessageRepository extends JpaRepository<CaseMessage, UUID> {

    @Query(value = """
            select new com.miabogado.domain.message.dto.MessageResponse(
                m.id, m.body, u.id, u.fullName, u.role, m.readAt, m.createdAt)
            from CaseMessage m
              join m.sender u
            where m.legalCase.id = :caseId
            order by m.createdAt desc
            """,
            countQuery = "select count(m) from CaseMessage m where m.legalCase.id = :caseId")
    Page<MessageResponse> findThread(@Param("caseId") UUID caseId, Pageable pageable);

    /**
     * Marca leido todo lo que el otro lado escribio. Un solo UPDATE en vez de
     * cargar el hilo entero para tocar unas cuantas filas.
     */
    @Modifying
    @Query("""
            update CaseMessage m set m.readAt = :now
            where m.legalCase.id = :caseId and m.readAt is null and m.sender.id <> :readerId
            """)
    int markThreadRead(@Param("caseId") UUID caseId,
                       @Param("readerId") UUID readerId,
                       @Param("now") Instant now);

    @Query("""
            select count(m) from CaseMessage m
            where m.legalCase.id = :caseId and m.readAt is null and m.sender.id <> :readerId
            """)
    long countUnread(@Param("caseId") UUID caseId, @Param("readerId") UUID readerId);
}
