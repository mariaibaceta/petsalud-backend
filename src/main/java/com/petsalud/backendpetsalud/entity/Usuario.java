package com.petsalud.backendpetsalud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 60)
    private String nombre;

    @Column(length = 60)
    private String apellido;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    // Relaciones
    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL)
    private List<Mascota> mascotas;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Venta> ventas;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Recompensa> recompensas;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Notificacion> notificaciones;
}