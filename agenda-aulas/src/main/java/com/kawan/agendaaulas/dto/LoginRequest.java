package com.kawan.agendaaulas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "Senha é obrigatória") String password
) {}
