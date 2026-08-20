package com.miabogado.domain.lawyer.entity;

import com.miabogado.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Especialidad del catalogo de plataforma (comun a todas las firmas).
 * No es tenant-scoped: si cada firma tuviera su lista, el directorio publico
 * de la Fase 8 no podria filtrar por especialidad entre firmas.
 */
@Entity
@Table(name = "practice_area")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeArea extends BaseEntity {

    @Column(nullable = false, length = 60, updatable = false)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
