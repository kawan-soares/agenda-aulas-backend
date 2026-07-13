package com.kawan.agendaaulas.dto;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "É preciso informar o horário que deseja reservar") Long availabilityId
) {}
