package com.miabogado.domain.settlement.controller;

import com.miabogado.domain.settlement.dto.LegalParameterResponse;
import com.miabogado.domain.settlement.dto.UpsertLegalParameterRequest;
import com.miabogado.domain.settlement.service.LegalParameterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mantenimiento del catalogo de parametros legales. Solo el super-admin: el
 * salario minimo no lo fija cada firma, y una firma que pudiera cambiarlo
 * podria inflar sus propias liquidaciones.
 */
@RestController
@RequestMapping("/api/v1/legal-parameters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class LegalParameterController {

    private final LegalParameterService legalParameterService;

    @GetMapping("/{year}")
    public LegalParameterResponse findByYear(@PathVariable int year) {
        return legalParameterService.findByYear(year);
    }

    /** Upsert: en diciembre solo importa que el ano entrante quede cargado. */
    @PutMapping
    public LegalParameterResponse upsert(@Valid @RequestBody UpsertLegalParameterRequest request) {
        return legalParameterService.upsert(request);
    }
}
