package com.petsalud.backendpetsalud.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 300)
    private String mensaje;

    private String tipo; // Ejemplos: "Promoción", "Recordatorio", "Evento"

    private LocalDateTime fecha;

    private Boolean leida;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
