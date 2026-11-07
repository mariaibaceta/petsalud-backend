package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.dto.UsuarioDtos.ActualizarUsuarioRequest;
import com.petsalud.backendpetsalud.dto.UsuarioDtos.RegistroRequest;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.exception.ReglaNegocioException;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registro público: la contraseña se guarda con BCrypt y el rol se FUERZA a CLIENTE.
     * El DTO no tiene id ni rol, por lo que no es posible sobrescribir cuentas ni escalar privilegios.
     */
    @Override
    @Transactional
    public Usuario registrarUsuario(RegistroRequest solicitud) {
        String email = normalizarEmail(solicitud.email());
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ReglaNegocioException("El email ya está registrado");
        }
        Usuario usuario = Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(solicitud.password()))
                .nombre(solicitud.nombre())
                .apellido(solicitud.apellido())
                .telefono(solicitud.telefono())
                .direccion(solicitud.direccion())
                .rol(Rol.CLIENTE)
                .build();
        return usuarioRepository.save(usuario);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email));
    }

    @Override
    @Transactional
    public Usuario actualizarUsuario(Long id, ActualizarUsuarioRequest solicitud) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setNombre(solicitud.nombre());
            usuario.setApellido(solicitud.apellido());
            usuario.setTelefono(solicitud.telefono());
            usuario.setDireccion(solicitud.direccion());
            // No se actualizan email, password ni rol por esta vía
            return usuarioRepository.save(usuario);
        }).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    /** Solo GERENTE (ver controlador). Un gerente no puede cambiar su propio rol (evita quedar sin administración). */
    @Override
    @Transactional
    public Usuario cambiarRol(Long id, Rol nuevoRol, Long idSolicitante) {
        if (id.equals(idSolicitante)) {
            throw new ReglaNegocioException("Un gerente no puede modificar su propio rol");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setRol(nuevoRol);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id, Long idSolicitante) {
        if (id.equals(idSolicitante)) {
            throw new ReglaNegocioException("Un gerente no puede eliminar su propia cuenta");
        }
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
