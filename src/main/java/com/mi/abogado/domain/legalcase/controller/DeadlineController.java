package com.mi.abogado.domain.legalcase.controller;

import com.mi.abogado.domain.legalcase.dto.CaseDeadlineResponse;
import com.mi.abogado.domain.legalcase.dto.UpcomingDeadline;
import com.mi.abogado.domain.legalcase.service.CaseDeadlineService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Agenda de vencimientos de la firma, transversal a los expedientes.
 * Es la pantalla de inicio natural del abogado: que vence esta semana.
 */
@Validated
@RestController
@RequestMapping("/api/v1/deadlines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class DeadlineController {

    private final CaseDeadlineService caseDeadlineService;

    /**
     * @param withinDays ventana hacia adelante; incluye lo ya vencido y pendiente
     * @param lawyerId   filtra por abogado; sin el, la agenda de toda la firma
     */
    @GetMapping("/upcoming")
    public List<UpcomingDeadline> upcoming(@RequestParam(defaultValue = "7") @Positive @Max(365) int withinDays,
                                           @RequestParam(required = false) UUID lawyerId) {
        return caseDeadlineService.findUpcoming(withinDays, lawyerId);
    }

    @PostMapping("/{id}/complete")
    public CaseDeadlineResponse complete(@PathVariable UUID id) {
        return caseDeadlineService.complete(id);
    }
}
