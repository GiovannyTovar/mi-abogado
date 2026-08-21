package com.miabogado.domain.settlement.repository;

import com.miabogado.domain.settlement.entity.LegalParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalParameterRepository extends JpaRepository<LegalParameter, UUID> {

    Optional<LegalParameter> findByYear(int year);

    /**
     * El ano pedido o, si aun no se ha cargado, el ultimo anterior disponible.
     * Evita que la calculadora deje de funcionar el 1 de enero solo porque
     * todavia no salio el decreto del salario minimo.
     */
    Optional<LegalParameter> findFirstByYearLessThanEqualOrderByYearDesc(int year);

    List<LegalParameter> findAllByOrderByYearDesc();
}
