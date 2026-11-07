package com.petsalud.backendpetsalud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "mascotas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String nombre;

    @Column(length = 30)
    private String especie;

    @Column(length = 50)
    private String raza;

    private LocalDate fechaNacimiento;

    @Column(length = 1)
    private String sexo;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario propietario;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    private List<Cita> citas;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    private List<ExpedienteMedico> expedientesMedicos;
}
