package com.kawan.agendaaulas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;

public record AvailabilityRequest(
        @NotNull @Future(message = "O horário precisa ser no futuro") LocalDateTime startTime,
        @NotNull @Future(message = "O horário precisa ser no futuro") LocalDateTime endTime
) {}
