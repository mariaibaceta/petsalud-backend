package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class MascotaDtos {

    private MascotaDtos() {
    }

    public record MascotaRequest(
            @NotBlank @Size(max = 50) @TextoSeguro String nombre,
            @NotBlank @Size(max = 30) @TextoSeguro String especie,
            @Size(max = 50) @TextoSeguro String raza,
            @PastOrPresent LocalDate fechaNacimiento,
            @Pattern(regexp = "^[MH]?$", message = "debe ser M o H") String sexo) {

        public Mascota aEntidad() {
            return Mascota.builder().nombre(nombre).especie(especie).raza(raza)
                    .fechaNacimiento(fechaNacimiento).sexo(sexo).build();
        }
    }

    public record MascotaResponse(Long id, String nombre, String especie, String raza, LocalDate fechaNacimiento,
                                  String sexo, Long propietarioId) {
        public static MascotaResponse de(Mascota m) {
            return new MascotaResponse(m.getId(), m.getNombre(), m.getEspecie(), m.getRaza(), m.getFechaNacimiento(),
                    m.getSexo(), m.getPropietario() != null ? m.getPropietario().getId() : null);
        }
    }
}
