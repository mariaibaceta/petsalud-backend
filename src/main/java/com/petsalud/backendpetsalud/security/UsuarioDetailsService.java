package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Carga los usuarios desde la base de datos (tabla usuarios) para Spring Security.
 * Se usa en el login (AuthenticationManager) y en cada request autenticado con JWT
 * (el filtro verifica que el usuario del token siga existiendo y toma su rol vigente).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                // Mensaje genérico: no se revela qué email se buscó (evita enumeración de usuarios).
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return new UsuarioDetails(usuario);
    }
}
