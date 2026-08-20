package com.miabogado.domain.client.controller;

import com.miabogado.domain.client.dto.ClientResponse;
import com.miabogado.domain.client.dto.ClientSummary;
import com.miabogado.domain.client.dto.CreateClientRequest;
import com.miabogado.domain.client.dto.UpdateClientRequest;
import com.miabogado.domain.client.entity.ClientStatus;
import com.miabogado.domain.client.service.ClientService;
import com.miabogado.domain.user.dto.UserResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public Page<ClientSummary> search(@RequestParam(required = false) ClientStatus status,
                                      @RequestParam(required = false) String search,
                                      @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
                                      Pageable pageable) {
        return clientService.search(status, search, pageable);
    }

    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable UUID id) {
        return clientService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        ClientResponse created = clientService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/clients/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PatchMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClientRequest request) {
        return clientService.update(id, request);
    }

    /** Invita al cliente al portal: se crea su usuario y entra con Google. */
    @PostMapping("/{id}/portal-access")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER')")
    public UserResponse grantPortalAccess(@PathVariable UUID id) {
        return clientService.grantPortalAccess(id);
    }

    @DeleteMapping("/{id}/portal-access")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER')")
    public ResponseEntity<Void> revokePortalAccess(@PathVariable UUID id) {
        clientService.revokePortalAccess(id);
        return ResponseEntity.noContent().build();
    }
}
