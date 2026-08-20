package com.miabogado.domain.portal.repository;

import com.miabogado.domain.legalcase.entity.LegalCase;
import com.miabogado.domain.portal.dto.PortalCaseSummary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Modelo de lectura del portal sobre {@code LegalCase}.
 * <p>
 * Vive aqui y no en {@code LegalCaseRepository} para que la dependencia apunte en
 * una sola direccion: el portal conoce el expediente, el expediente no conoce el
 * portal. Ademas mantiene junto todo lo que el cliente puede leer, que es lo que
 * hay que revisar cuando se audita el aislamiento.
 * <p>
 * Extiende {@code Repository} y no {@code JpaRepository} a proposito: desde el
 * portal no se guarda ni se borra nada.
 */
public interface PortalCaseRepository extends Repository<LegalCase, UUID> {

    /**
     * Casos del cliente, con el contador de mensajes que le escribio la firma y
     * aun no ha leido.
     */
    @Query("""
            select new com.miabogado.domain.portal.dto.PortalCaseSummary(
                lc.id, lc.caseNumber, lc.title, lc.caseType, lc.status, u.fullName, lc.openedAt,
                (select count(m) from CaseMessage m
                  where m.legalCase = lc and m.readAt is null and m.sender.id <> :readerId))
            from LegalCase lc
              left join lc.assignedLawyer l
              left join l.user u
            where lc.client.id = :clientId
            order by lc.openedAt desc
            """)
    List<PortalCaseSummary> findCasesOf(@Param("clientId") UUID clientId,
                                        @Param("readerId") UUID readerId);

    /** Un caso concreto, comprobando de paso que sea de ese cliente. */
    @EntityGraph(attributePaths = {"assignedLawyer", "assignedLawyer.user", "practiceArea"})
    Optional<LegalCase> findByIdAndClient_Id(UUID id, UUID clientId);
}
