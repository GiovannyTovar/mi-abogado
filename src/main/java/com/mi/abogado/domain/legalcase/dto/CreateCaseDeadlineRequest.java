package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.DeadlineType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCaseDeadlineRequest(
        @NotNull DeadlineType deadlineType,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull LocalDate dueDate,
        @PositiveOrZero @Max(60) Short notifyDaysBefore
) {
}
