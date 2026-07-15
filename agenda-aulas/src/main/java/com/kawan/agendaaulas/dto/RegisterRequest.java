package com.kawan.agendaaulas.dto;

import com.kawan.agendaaulas.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotBlank @Email(message = "E-mail inválido") String email,
        @NotBlank @Size(min = 6, message = "Senha precisa ter no mínimo 6 caracteres") String password,
        @NotNull(message = "Papel (TEACHER ou STUDENT) é obrigatório") Role role
) {}
