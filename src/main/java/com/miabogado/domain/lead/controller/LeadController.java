package com.miabogado.domain.lead.controller;

import com.miabogado.domain.lead.dto.ConvertLeadRequest;
import com.miabogado.domain.lead.dto.CreateLeadRequest;
import com.miabogado.domain.lead.dto.LeadPipelineStage;
import com.miabogado.domain.lead.dto.LeadResponse;
import com.miabogado.domain.lead.dto.LeadSummary;
import com.miabogado.domain.lead.dto.UpdateLeadRequest;
import com.miabogado.domain.lead.entity.LeadSource;
import com.miabogado.domain.lead.entity.LeadStatus;
import com.miabogado.domain.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public Page<LeadSummary> search(@RequestParam(required = false) LeadStatus status,
                                    @RequestParam(required = false) LeadSource source,
                                    @RequestParam(required = false) UUID lawyerId,
                                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                    Pageable pageable) {
        return leadService.search(status, source, lawyerId, pageable);
    }

    /** Tablero: cuantos leads hay en cada etapa. */
    @GetMapping("/pipeline")
    public List<LeadPipelineStage> pipeline() {
        return leadService.pipeline();
    }

    @GetMapping("/{id}")
    public LeadResponse findById(@PathVariable UUID id) {
        return leadService.findById(id);
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody CreateLeadRequest request,
                                               UriComponentsBuilder uriBuilder) {
        LeadResponse created = leadService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/leads/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PatchMapping("/{id}")
    public LeadResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLeadRequest request) {
        return leadService.update(id, request);
    }

    @PostMapping("/{id}/contacted")
    public LeadResponse markContacted(@PathVariable UUID id) {
        return leadService.markContacted(id);
    }

    @PostMapping("/{id}/convert")
    public LeadResponse convert(@PathVariable UUID id, @Valid @RequestBody ConvertLeadRequest request) {
        return leadService.convert(id, request);
    }

    @PostMapping("/{id}/lost")
    public LeadResponse markLost(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return leadService.markLost(id, reason);
    }
}
