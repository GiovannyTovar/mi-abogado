package com.mi.abogado.domain.legalcase.controller;

import com.mi.abogado.domain.legalcase.dto.CaseDeadlineResponse;
import com.mi.abogado.domain.legalcase.dto.CaseEventResponse;
import com.mi.abogado.domain.legalcase.dto.CaseResponse;
import com.mi.abogado.domain.legalcase.dto.CaseSummary;
import com.mi.abogado.domain.legalcase.dto.CloseCaseRequest;
import com.mi.abogado.domain.legalcase.dto.CreateCaseDeadlineRequest;
import com.mi.abogado.domain.legalcase.dto.CreateCaseEventRequest;
import com.mi.abogado.domain.legalcase.dto.CreateCaseRequest;
import com.mi.abogado.domain.legalcase.dto.UpdateCaseRequest;
import com.mi.abogado.domain.legalcase.entity.CaseStatus;
import com.mi.abogado.domain.legalcase.entity.CaseType;
import com.mi.abogado.domain.legalcase.service.CaseDeadlineService;
import com.mi.abogado.domain.legalcase.service.CaseEventService;
import com.mi.abogado.domain.legalcase.service.LegalCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class LegalCaseController {

    private final LegalCaseService legalCaseService;
    private final CaseEventService caseEventService;
    private final CaseDeadlineService caseDeadlineService;

    // --- Expediente ---

    @GetMapping
    public Page<CaseSummary> search(@RequestParam(required = false) CaseStatus status,
                                    @RequestParam(required = false) CaseType caseType,
                                    @RequestParam(required = false) UUID clientId,
                                    @RequestParam(required = false) UUID lawyerId,
                                    @RequestParam(required = false) String search,
                                    @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC)
                                    Pageable pageable) {
        return legalCaseService.search(status, caseType, clientId, lawyerId, search, pageable);
    }

    @GetMapping("/{id}")
    public CaseResponse findById(@PathVariable UUID id) {
        return legalCaseService.findById(id);
    }

    @PostMapping
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CreateCaseRequest request,
                                               UriComponentsBuilder uriBuilder) {
        CaseResponse created = legalCaseService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/cases/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PatchMapping("/{id}")
    public CaseResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCaseRequest request) {
        return legalCaseService.update(id, request);
    }

    @PostMapping("/{id}/close")
    public CaseResponse close(@PathVariable UUID id, @Valid @RequestBody CloseCaseRequest request) {
        return legalCaseService.close(id, request);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER')")
    public CaseResponse reopen(@PathVariable UUID id) {
        return legalCaseService.reopen(id);
    }

    // --- Bitacora ---

    @GetMapping("/{id}/events")
    public Page<CaseEventResponse> events(@PathVariable UUID id,
                                          @PageableDefault(size = 30) Pageable pageable) {
        return caseEventService.findByCase(id, pageable);
    }

    @PostMapping("/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public void addEvent(@PathVariable UUID id, @Valid @RequestBody CreateCaseEventRequest request) {
        caseEventService.addManualEvent(id, request);
    }

    // --- Terminos ---

    @GetMapping("/{id}/deadlines")
    public List<CaseDeadlineResponse> deadlines(@PathVariable UUID id) {
        return caseDeadlineService.findByCase(id);
    }

    @PostMapping("/{id}/deadlines")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseDeadlineResponse addDeadline(@PathVariable UUID id,
                                            @Valid @RequestBody CreateCaseDeadlineRequest request) {
        return caseDeadlineService.create(id, request);
    }
}
