package com.mi.abogado.domain.lawyer.repository;

import com.mi.abogado.domain.lawyer.dto.LawyerSummary;
import com.mi.abogado.domain.lawyer.entity.Lawyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Todas las consultas de esta interfaz salen ya filtradas por {@code tenant_id}:
 * Lawyer hereda de TenantScopedEntity y Hibernate anade la condicion. No hace
 * falta (ni debe) repetirla a mano.
 */
public interface LawyerRepository extends JpaRepository<Lawyer, UUID> {

    /**
     * Detalle: una sola consulta con usuario y especialidades resueltos.
     * Sin el EntityGraph serian 3 viajes a la BD (el clasico N+1).
     */
    @EntityGraph(attributePaths = {"user", "practiceAreas"})
    Optional<Lawyer> findWithDetailsById(UUID id);

    /**
     * Listado paginado como proyeccion: no materializa entidades ni colecciones.
     */
    @Query(value = """
            select new com.mi.abogado.domain.lawyer.dto.LawyerSummary(
                l.id, u.fullName, u.email, u.photoUrl, l.licenseNumber,
                l.city, l.yearsOfExperience, l.published, l.ratingAvg)
            from Lawyer l
              join l.user u
            where (:city is null or lower(l.city) = lower(:city))
              and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(l)
            from Lawyer l
              join l.user u
            where (:city is null or lower(l.city) = lower(:city))
              and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%')))
            """)
    Page<LawyerSummary> search(@Param("city") String city,
                               @Param("search") String search,
                               Pageable pageable);

    boolean existsByLicenseNumber(String licenseNumber);

    Optional<Lawyer> findByUser_Id(UUID userId);
}
