package com.miabogado.domain.lawyer.controller;

import com.miabogado.domain.lawyer.dto.CreateLawyerRequest;
import com.miabogado.domain.lawyer.dto.LawyerResponse;
import com.miabogado.domain.lawyer.dto.LawyerSummary;
import com.miabogado.domain.lawyer.dto.UpdateLawyerRequest;
import com.miabogado.domain.lawyer.service.LawyerService;
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

import java.util.UUID;

/**
 * Sin logica de negocio: valida la entrada, delega y traduce a HTTP.
 * El filtro por firma no aparece aqui — lo aplica Hibernate con el tenant del token.
 */
@RestController
@RequestMapping("/api/v1/lawyers")
@RequiredArgsConstructor
public class LawyerController {

    private final LawyerService lawyerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
    public Page<LawyerSummary> search(@RequestParam(required = false) String city,
                                      @RequestParam(required = false) String search,
                                      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                      Pageable pageable) {
        return lawyerService.search(city, search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
    public LawyerResponse findById(@PathVariable UUID id) {
        return lawyerService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('FIRM_OWNER')")
    public ResponseEntity<LawyerResponse> create(@Valid @RequestBody CreateLawyerRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        LawyerResponse created = lawyerService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/lawyers/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER')")
    public LawyerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLawyerRequest request) {
        return lawyerService.update(id, request);
    }
}
