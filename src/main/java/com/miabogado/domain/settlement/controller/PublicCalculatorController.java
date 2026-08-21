package com.miabogado.domain.settlement.controller;

import com.miabogado.domain.settlement.dto.LegalParameterResponse;
import com.miabogado.domain.settlement.dto.SettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementResult;
import com.miabogado.domain.settlement.service.LegalParameterService;
import com.miabogado.domain.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * La calculadora abierta de la landing. Es el gancho del producto: el trabajador
 * llega buscando cuanto le deben y sale con una cifra y con un abogado.
 * <p>
 * No guarda nada y no pide sesion. Un calculo anonimo no pertenece a ninguna
 * firma, asi que aqui no hay tenant y no puede haberlo: convertir la consulta en
 * lead es cosa del marketplace (Fase 8), que si sabe a que firma va.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicCalculatorController {

    private final SettlementService settlementService;
    private final LegalParameterService legalParameterService;

    @PostMapping("/calculator/settlement")
    public SettlementResult calculate(@Valid @RequestBody SettlementRequest request) {
        return settlementService.preview(request);
    }

    /** La landing pinta el SMLMV y el auxilio vigentes sin tener que preguntar. */
    @GetMapping("/legal-parameters")
    public List<LegalParameterResponse> listParameters() {
        return legalParameterService.findAll();
    }
}
