package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre la entidad {@link Usuario} y Spring Security.
 * Solo conserva los datos necesarios para autenticar y autorizar (id, email, hash y rol):
 * no mantiene referencia a la entidad JPA para no arrastrar relaciones ni datos personales.
 */
public class UsuarioDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Rol rol;

    public UsuarioDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.passwordHash = usuario.getPassword();
        this.rol = usuario.getRol();
    }

    public Long getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    /** Autoridad con prefijo ROLE_ para que funcionen hasRole('...') y @PreAuthorize("hasRole('...')"). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
