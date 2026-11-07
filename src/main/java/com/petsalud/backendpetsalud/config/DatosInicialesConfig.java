package com.petsalud.backendpetsalud.config;

import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de demostración SOLO para los perfiles dev y test (nunca en producción).
 * Crea un usuario por rol (más un segundo cliente para probar IDOR) con contraseña en BCrypt.
 * La contraseña de demostración se toma de DEMO_PASSWORD (ver README); si no existe se genera una aleatoria.
 */
@Configuration
@Profile({"dev", "test"})
public class DatosInicialesConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DatosInicialesConfig.class);

    @Bean
    CommandLineRunner cargarDatosIniciales(UsuarioRepository usuarios, MascotaRepository mascotas,
                                           ExpedienteMedicoRepository expedientes, ProductoRepository productos,
                                           VentaRepository ventas, RecompensaRepository recompensas,
                                           NotificacionRepository notificaciones, CitaRepository citas,
                                           PasswordEncoder encoder,
                                           @Value("${seguridad.demo.password:}") String claveDemoConfigurada) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) {
                if (usuarios.count() > 0) {
                    return;
                }
                String claveDemo = claveDemoConfigurada;
                if (claveDemo == null || claveDemo.isBlank()) {
                    claveDemo = "Demo-" + java.util.UUID.randomUUID().toString().substring(0, 12) + "9";
                    LOG.warn("DEMO_PASSWORD no definida: contraseña aleatoria de demostración = {}", claveDemo);
                }
                // Un hash por usuario: BCrypt genera una sal aleatoria en cada encode()
                Usuario gerente = usuarios.save(usuario("gerente@petsalud.cl", encoder.encode(claveDemo), "Gabriela", "Rojas", Rol.GERENTE));
                Usuario empleado = usuarios.save(usuario("empleado@petsalud.cl", encoder.encode(claveDemo), "Víctor", "Soto", Rol.EMPLEADO));
                Usuario ana = usuarios.save(usuario("ana@cliente.cl", encoder.encode(claveDemo), "Ana", "Pérez", Rol.CLIENTE));
                Usuario bruno = usuarios.save(usuario("bruno@cliente.cl", encoder.encode(claveDemo), "Bruno", "Díaz", Rol.CLIENTE));

                Mascota luna = mascotas.save(Mascota.builder().nombre("Luna").especie("Perro").raza("Quiltro")
                        .fechaNacimiento(LocalDate.of(2021, 3, 14)).sexo("H").propietario(ana).build());
                Mascota michi = mascotas.save(Mascota.builder().nombre("Michi").especie("Gato").raza("Persa")
                        .fechaNacimiento(LocalDate.of(2019, 8, 2)).sexo("M").propietario(bruno).build());

                expedientes.save(ExpedienteMedico.builder().mascota(luna).veterinario(empleado)
                        .fecha(LocalDateTime.now().minusDays(30)).descripcion("Vacunación anual")
                        .diagnostico("Sano").tratamiento("Vacuna óctuple").build());
                expedientes.save(ExpedienteMedico.builder().mascota(michi).veterinario(empleado)
                        .fecha(LocalDateTime.now().minusDays(10)).descripcion("Control por picazón de oído")
                        .diagnostico("Otitis externa").tratamiento("Gotas óticas por 7 días").build());

                citas.save(Cita.builder().mascota(luna).veterinario(empleado).estado("Agendada")
                        .fechaHora(LocalDateTime.now().plusDays(7).withNano(0)).motivo("Control general").build());
                citas.save(Cita.builder().mascota(michi).veterinario(empleado).estado("Agendada")
                        .fechaHora(LocalDateTime.now().plusDays(3).withNano(0)).motivo("Revisión otitis").build());

                Producto alimento = productos.save(Producto.builder().nombre("Alimento Premium Adulto 15 kg")
                        .descripcion("Alimento seco para perros adultos").precio(new BigDecimal("45990"))
                        .stock(20).categoria("Alimentos").build());
                productos.save(Producto.builder().nombre("Pelota de caucho").descripcion("Juguete resistente")
                        .precio(new BigDecimal("3990")).stock(50).categoria("Juguetes").build());
                productos.save(Producto.builder().nombre("Antiparasitario interno").descripcion("Comprimido")
                        .precio(new BigDecimal("8990")).stock(4).categoria("Suplementos").build());

                ventas.save(Venta.builder().usuario(bruno).productos(List.of(alimento))
                        .total(alimento.getPrecio()).fecha(LocalDateTime.now().minusDays(2)).build());
                recompensas.save(Recompensa.builder().usuario(bruno).puntos(460)
                        .descripcion("Puntos por compra").fecha(LocalDateTime.now().minusDays(2)).build());
                notificaciones.save(Notificacion.builder().usuario(ana).mensaje("Recuerde la cita de control de Luna")
                        .tipo("Recordatorio").fecha(LocalDateTime.now()).leida(false).build());
                notificaciones.save(Notificacion.builder().usuario(bruno).mensaje("20% de descuento en alimentos")
                        .tipo("Promoción").fecha(LocalDateTime.now()).leida(false).build());

                LOG.info("Datos de demostración cargados: gerente={}, empleado={}, clientes={},{}",
                        gerente.getId(), empleado.getId(), ana.getId(), bruno.getId());
            }
        };
    }

    private static Usuario usuario(String email, String hash, String nombre, String apellido, Rol rol) {
        return Usuario.builder().email(email).password(hash).nombre(nombre).apellido(apellido)
                .telefono("+56900000000").direccion("Santiago").rol(rol).build();
    }
}
