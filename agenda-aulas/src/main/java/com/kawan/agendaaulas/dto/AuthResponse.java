package com.kawan.agendaaulas.dto;

import com.kawan.agendaaulas.model.Role;

public record AuthResponse(
        String token,
        String name,
        String email,
        Role role
) {}
