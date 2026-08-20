package com.miabogado.domain.tenant.repository;

import com.miabogado.domain.tenant.dto.TenantSummary;
import com.miabogado.domain.tenant.entity.Tenant;
import com.miabogado.domain.tenant.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Panel del super-admin: una fila por firma con el nombre de su plan vigente.
     * Proyeccion con join explicito a Subscription — Tenant no la referencia, y no
     * tiene por que hacerlo solo para este listado.
     */
    @Query(value = """
            select new com.miabogado.domain.tenant.dto.TenantSummary(
                t.id, t.name, t.slug, t.status, p.name, t.createdAt)
            from Tenant t
              left join Subscription s on s.tenant = t and s.status <> 'CANCELLED'
              left join s.plan p
            where (:status is null or t.status = :status)
              and (:search is null or lower(t.name) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(t)
            from Tenant t
            where (:status is null or t.status = :status)
              and (:search is null or lower(t.name) like lower(concat('%', :search, '%')))
            """)
    Page<TenantSummary> search(@Param("status") TenantStatus status,
                               @Param("search") String search,
                               Pageable pageable);
}
