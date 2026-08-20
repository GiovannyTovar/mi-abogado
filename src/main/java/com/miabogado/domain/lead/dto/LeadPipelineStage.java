package com.miabogado.domain.lead.dto;

import com.miabogado.domain.lead.entity.LeadStatus;

/** Conteo por etapa para el tablero del pipeline. */
public record LeadPipelineStage(LeadStatus status, long total) {
}
