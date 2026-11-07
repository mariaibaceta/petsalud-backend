package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.dto.UsuarioDtos.ActualizarUsuarioRequest;
import com.petsalud.backendpetsalud.dto.UsuarioDtos.RegistroRequest;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    Usuario registrarUsuario(RegistroRequest solicitud);
    Optional<Usuario> obtenerUsuarioPorId(Long id);
    Optional<Usuario> obtenerUsuarioPorEmail(String email);
    Usuario actualizarUsuario(Long id, ActualizarUsuarioRequest solicitud);
    Usuario cambiarRol(Long id, Rol nuevoRol, Long idSolicitante);
    void eliminarUsuario(Long id, Long idSolicitante);
    List<Usuario> listarUsuarios();
}
