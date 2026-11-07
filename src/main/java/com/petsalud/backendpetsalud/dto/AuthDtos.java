package com.petsalud.backendpetsalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs del flujo de autenticación. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    /** Respuesta del login: token Bearer, expiración (segundos) y datos mínimos del usuario. */
    public record LoginResponse(String token, String tipo, long expiraEnSegundos, Long usuarioId, String email,
                                String rol) {
    }
}
