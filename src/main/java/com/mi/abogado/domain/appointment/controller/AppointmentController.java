package com.mi.abogado.domain.appointment.controller;

import com.mi.abogado.domain.appointment.dto.AppointmentResponse;
import com.mi.abogado.domain.appointment.dto.CreateAppointmentRequest;
import com.mi.abogado.domain.appointment.dto.UpdateAppointmentRequest;
import com.mi.abogado.domain.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /** Agenda entre dos instantes; el frontend manda la semana o el mes que pinta. */
    @GetMapping
    public List<AppointmentResponse> agenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                            @RequestParam(required = false) UUID lawyerId,
                                            @RequestParam(required = false) UUID clientId) {
        return appointmentService.agenda(from, to, lawyerId, clientId);
    }

    @GetMapping("/{id}")
    public AppointmentResponse findById(@PathVariable UUID id) {
        return appointmentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        AppointmentResponse created = appointmentService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/appointments/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PatchMapping("/{id}")
    public AppointmentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentRequest request) {
        return appointmentService.update(id, request);
    }

    @PostMapping("/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable UUID id) {
        return appointmentService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return appointmentService.cancel(id, reason);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable UUID id) {
        return appointmentService.complete(id);
    }
}
