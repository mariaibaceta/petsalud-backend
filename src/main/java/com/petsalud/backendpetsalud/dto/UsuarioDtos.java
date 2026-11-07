package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTOs de usuario. El registro NO recibe rol ni id (evita escalada de privilegios y asignación masiva). */
public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record RegistroRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener al menos una letra y un número")
            String password,
            @NotBlank @Size(max = 60) @TextoSeguro String nombre,
            @Size(max = 60) @TextoSeguro String apellido,
            @Size(max = 20) @Pattern(regexp = "^[+0-9 ]*$", message = "solo dígitos, espacios y +") String telefono,
            @Size(max = 150) @TextoSeguro String direccion) {
    }

    public record ActualizarUsuarioRequest(
            @NotBlank @Size(max = 60) @TextoSeguro String nombre,
            @Size(max = 60) @TextoSeguro String apellido,
            @Size(max = 20) @Pattern(regexp = "^[+0-9 ]*$", message = "solo dígitos, espacios y +") String telefono,
            @Size(max = 150) @TextoSeguro String direccion) {
    }

    public record CambioRolRequest(@NotNull Rol rol) {
    }

    /** Vista pública del usuario: nunca incluye la contraseña ni sus relaciones. */
    public record UsuarioResponse(Long id, String email, String nombre, String apellido, String telefono,
                                  String direccion, Rol rol) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getEmail(), u.getNombre(), u.getApellido(), u.getTelefono(),
                    u.getDireccion(), u.getRol());
        }
    }
}
