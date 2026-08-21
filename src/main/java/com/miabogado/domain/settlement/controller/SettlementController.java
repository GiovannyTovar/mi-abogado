package com.miabogado.domain.settlement.controller;

import com.miabogado.domain.settlement.dto.SaveSettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementResponse;
import com.miabogado.domain.settlement.dto.SettlementResult;
import com.miabogado.domain.settlement.dto.SettlementSummary;
import com.miabogado.domain.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * Calculadora de la firma. Misma cuenta que la publica, con dos diferencias que
 * son las que la hacen util en el despacho: el resultado se guarda y queda
 * colgado del cliente y del expediente.
 */
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public Page<SettlementSummary> search(@RequestParam(required = false) UUID clientId,
                                          @RequestParam(required = false) UUID caseId,
                                          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                          Pageable pageable) {
        return settlementService.search(clientId, caseId, pageable);
    }

    @GetMapping("/{id}")
    public SettlementResponse findById(@PathVariable UUID id) {
        return settlementService.findById(id);
    }

    /** Tanteo sin guardar: la firma ajusta cifras antes de dejar constancia. */
    @PostMapping("/preview")
    public SettlementResult preview(@Valid @RequestBody SettlementRequest request) {
        return settlementService.preview(request);
    }

    @PostMapping
    public ResponseEntity<SettlementResponse> create(@Valid @RequestBody SaveSettlementRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        SettlementResponse created = settlementService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/settlements/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    /** Una liquidacion no se corrige: se borra y se calcula otra vez. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        settlementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
