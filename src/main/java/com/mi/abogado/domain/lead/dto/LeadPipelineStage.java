package com.mi.abogado.domain.lead.dto;

import com.mi.abogado.domain.lead.entity.LeadStatus;

/** Conteo por etapa para el tablero del pipeline. */
public record LeadPipelineStage(LeadStatus status, long total) {
}
