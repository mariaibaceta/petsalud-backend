package com.petsalud.backendpetsalud.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recompensas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer puntos;

    @Column(length = 200)
    private String descripcion;

    private LocalDateTime fecha;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
