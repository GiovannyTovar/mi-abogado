package com.miabogado.domain.lawyer.entity;

import com.miabogado.domain.user.entity.User;
import com.miabogado.shared.persistence.TenantScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Perfil profesional del abogado dentro de una firma.
 * <p>
 * Extiende a {@link User} en lugar de duplicar sus datos: nombre, email y foto
 * viven en la identidad; aqui van tarjeta profesional, experiencia y ficha publica.
 */
@Entity
@Table(name = "lawyer")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lawyer extends TenantScopedEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    /** Tarjeta profesional (Consejo Superior de la Judicatura). */
    @Column(name = "license_number", nullable = false, length = 40)
    private String licenseNumber;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "years_of_experience", nullable = false)
    private short yearsOfExperience;

    @Column(length = 100)
    private String city;

    /** Ficha en el directorio publico: /abogados/{publicSlug}. Fase 8. */
    @Column(name = "public_slug", length = 140)
    private String publicSlug;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "hourly_rate", precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    /** Lo calcula el modulo de resenas; no se edita desde el CRUD del perfil. */
    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lawyer_practice_area",
            joinColumns = @JoinColumn(name = "lawyer_id"),
            inverseJoinColumns = @JoinColumn(name = "practice_area_id"))
    private Set<PracticeArea> practiceAreas = new LinkedHashSet<>();

    public Lawyer(User user, String licenseNumber) {
        this.user = user;
        this.licenseNumber = licenseNumber;
    }

    public void replacePracticeAreas(Set<PracticeArea> areas) {
        practiceAreas.clear();
        practiceAreas.addAll(areas);
    }

    /**
     * Publicar en el directorio exige ciudad y slug (lo respalda ck_lawyer_publishable).
     */
    public void publish(String publicSlug) {
        if (city == null || city.isBlank()) {
            throw new IllegalStateException("Un perfil sin ciudad no puede publicarse");
        }
        this.publicSlug = publicSlug;
        this.published = true;
    }

    public void unpublish() {
        this.published = false;
    }
}
