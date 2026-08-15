package com.mi.abogado.domain.client.repository;

import com.mi.abogado.domain.client.dto.ClientSummary;
import com.mi.abogado.domain.client.entity.Client;
import com.mi.abogado.domain.client.entity.ClientStatus;
import com.mi.abogado.domain.client.entity.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Filtrado por tenant automatico: Client hereda de TenantScopedEntity.
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    /**
     * Listado del CRM con el conteo de casos abiertos resuelto en la misma
     * consulta (subconsulta correlacionada, no una llamada por fila).
     */
    @Query(value = """
            select new com.mi.abogado.domain.client.dto.ClientSummary(
                c.id, c.clientType, c.documentType, c.documentNumber, c.name,
                c.email, c.phone, c.city, c.status,
                (select count(lc) from LegalCase lc
                  where lc.client = c and lc.status <> com.mi.abogado.domain.legalcase.entity.CaseStatus.CLOSED))
            from Client c
            where (:status is null or c.status = :status)
              and (:search is null
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or c.documentNumber like concat('%', :search, '%'))
            """,
            countQuery = """
            select count(c) from Client c
            where (:status is null or c.status = :status)
              and (:search is null
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or c.documentNumber like concat('%', :search, '%'))
            """)
    Page<ClientSummary> search(@Param("status") ClientStatus status,
                               @Param("search") String search,
                               Pageable pageable);
}
