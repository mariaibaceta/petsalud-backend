package com.petsalud.backendpetsalud.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Configuración central de Spring Security para una API REST stateless con JWT.
 * <p>
 * Defensa en profundidad: (1) reglas por URL en este SecurityFilterChain y (2) {@code @PreAuthorize}
 * en cada método de controlador (habilitado con {@link EnableMethodSecurity}), incluida la verificación
 * de propiedad de recursos para el rol CLIENTE.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint entryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, JwtAuthenticationEntryPoint entryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF desactivado de forma justificada: API stateless, sin cookies de sesión y con el JWT
                // en la cabecera Authorization, que el navegador NO adjunta automáticamente en peticiones
                // de otro sitio. Si el token se moviera a una cookie, CSRF debe reactivarse (+ SameSite).
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Cabeceras de seguridad (mitigan XSS reflejado/clickjacking/sniffing en clientes navegador).
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> { })
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .cacheControl(c -> { }))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/usuarios/registro").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Operaciones exclusivas de gerencia (1.ª capa; @PreAuthorize es la 2.ª)
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/*", "/api/expedientes/*",
                                "/api/productos/*", "/api/recompensas/*").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/rol").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/ventas").hasRole("GERENTE")
                        // Operaciones de personal (EMPLEADO o GERENTE)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios", "/api/citas/veterinario/*",
                                "/api/citas/entre-fechas", "/api/productos/stock-bajo").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes", "/api/productos",
                                "/api/recompensas/usuario/*", "/api/notificaciones/usuario/*").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/*", "/api/citas/*/estado").hasAnyRole("EMPLEADO", "GERENTE")
                        // Resto de la API: requiere JWT válido (la propiedad se verifica con @PreAuthorize)
                        .requestMatchers("/api/**").authenticated()
                        // Cualquier otra ruta: denegada por defecto
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Evita que Spring Boot registre el filtro JWT una segunda vez como filtro de servlet global. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> desactivarRegistroAutomaticoJwt(JwtAuthenticationFilter filtro) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /** BCrypt con factor de costo configurable (12 por defecto). */
    @Bean
    public PasswordEncoder passwordEncoder(@Value("${seguridad.bcrypt.fuerza}") int fuerza) {
        return new BCryptPasswordEncoder(fuerza);
    }

    /** Autenticación usuario/contraseña contra la base de datos (UsuarioDetailsService + BCrypt). */
    @Bean
    public AuthenticationManager authenticationManager(UsuarioDetailsService usuarioDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarioDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(proveedor);
    }

    /** CORS restringido a los orígenes configurados (propiedad seguridad.cors.origenes-permitidos). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${seguridad.cors.origenes-permitidos}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false); // el JWT no viaja en cookies
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
