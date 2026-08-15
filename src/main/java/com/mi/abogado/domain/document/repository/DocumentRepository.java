package com.mi.abogado.domain.document.repository;

import com.mi.abogado.domain.document.entity.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @EntityGraph(attributePaths = {"legalCase", "uploadedBy"})
    Optional<Document> findWithDetailsById(UUID id);

    /** Vista de la firma: todos los archivos del expediente. */
    @EntityGraph(attributePaths = "uploadedBy")
    List<Document> findByLegalCase_IdOrderByCreatedAtDesc(UUID caseId);

    /** Vista del portal: solo lo que la firma decidio compartir. */
    @EntityGraph(attributePaths = "uploadedBy")
    @Query("""
            select d from Document d
            where d.legalCase.id = :caseId
              and d.visibility = com.mi.abogado.domain.document.entity.DocumentVisibility.SHARED_WITH_CLIENT
            order by d.createdAt desc
            """)
    List<Document> findSharedByCaseId(@Param("caseId") UUID caseId);
}
