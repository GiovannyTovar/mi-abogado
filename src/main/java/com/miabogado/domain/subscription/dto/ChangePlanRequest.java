package com.miabogado.domain.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangePlanRequest(@NotNull UUID planId) {
}
