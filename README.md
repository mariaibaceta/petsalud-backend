# Clínica PetSalud: backend seguro (Spring Security + JWT)

Este repositorio contiene el backend REST de **Clínica PetSalud**, una veterinaria con 4 sucursales. Gestiona usuarios, mascotas, citas, expedientes médicos, productos, ventas, recompensas y notificaciones.

Sobre el backend base entregado se integró una capa de seguridad completa:
- autenticación **JWT stateless**;
- autorización por roles **CLIENTE / EMPLEADO / GERENTE** con control de propiedad de los recursos;
- protección frente a **SQL Injection, XSS y CSRF**;
- bloqueo ante fuerza bruta;
- monitoreo de actividad sospechosa.

> Proyecto académico: Desarrollo Backend II (PBY2202), sumativa de la semana 3, forma B.

## Contenido

- [Stack](#stack)
- [Requisitos](#requisitos)
- [Configuración (variables de entorno)](#configuración-variables-de-entorno)
- [Ejecución](#ejecución)
- [Pruebas](#pruebas)
- [Usuarios de prueba](#usuarios-de-prueba-solo-desarrollo)
- [Flujo de autenticación JWT](#flujo-de-autenticación-jwt)
- [Matriz de roles y endpoints](#matriz-de-roles-y-endpoints)
- [Medidas de seguridad por amenaza](#medidas-de-seguridad-por-amenaza)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Autor](#autor)

## Stack

| Componente | Versión |
|---|---|
| Java | 17 o superior (probado con 21) |
| Spring Boot | **4.1.2** (actualizado desde 3.3.6, que no tiene soporte OSS) |
| Spring Security | 7.1.2 |
| jjwt (io.jsonwebtoken) | 0.13.0 |
| Hibernate ORM | 7.4 |
| Base de datos | MySQL 8.x (ejecución) · H2 en memoria (pruebas) |
| Validación / XSS | Jakarta Bean Validation + OWASP Java HTML Sanitizer 20261103.2 |
| Build | Maven Wrapper (Maven 3.9.9) |

**Sobre la versión de Spring Boot.** El proyecto base usaba Spring Boot 3.3.6, cuya línea 3.3.x dejó de tener soporte OSS el 30-06-2025 y, por lo tanto, ya no recibe parches de seguridad. Se actualizó a 4.1.2, la versión vigente según el calendario oficial de Spring (soporte OSS hasta el 31-07-2027). En Boot 4 el starter `spring-boot-starter-web` quedó reemplazado por `spring-boot-starter-webmvc`, que cumple el mismo propósito (Spring MVC + Tomcat embebido). Por eso el `pom.xml` usa `spring-boot-starter-webmvc` en lugar del `spring-boot-starter-web` que nombra el enunciado.

## Requisitos

- JDK 17 o superior.
- MySQL 8.x. Para levantarlo con Docker:

  ```bash
  docker run -d --name petsalud-mysql -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=<root> -e MYSQL_DATABASE=petsalud_db \
    -e MYSQL_USER=petsalud_app -e MYSQL_PASSWORD=<clave> mysql:8.4
  ```

- Para las pruebas **no se necesita MySQL**.

## Configuración (variables de entorno)

En el código no hay ningún secreto. Copie `.env.example` como `.env` (archivo excluido de git) y complete los valores. Spring Boot lo carga automáticamente. También puede definirlos como variables de entorno del sistema.

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC | `jdbc:mysql://localhost:3306/petsalud_db?sslMode=PREFERRED&serverTimezone=UTC` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de MySQL | `petsalud_app` / `***` |
| `JWT_SECRET` | **Base64 de al menos 32 bytes (256 bits)**. Si falta o es corto, la app no arranca | `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | Vigencia del token | `60` |
| `JWT_ISSUER` | Emisor (claim `iss`) | `petsalud-backend` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos, separados por coma | `http://localhost:4200` |
| `SPRING_PROFILES_ACTIVE` | `dev` (carga los datos de demostración) o `prod` | `dev` |
| `DEMO_PASSWORD` | Contraseña de los usuarios demo (solo dev). Si se deja vacía, se genera una aleatoria y se muestra en consola | `PetSalud#2026` |

## Ejecución

```bash
./mvnw spring-boot:run          # http://localhost:8080
```

## Pruebas

```bash
./mvnw test                     # 365 pruebas (JUnit 5 + MockMvc + Spring Security Test), con H2 en memoria
```

Además, las pruebas de seguridad se ejecutaron manualmente con Postman (Collection Runner) contra la aplicación levantada con MySQL. Los resultados están en el informe técnico.

## Usuarios de prueba (solo desarrollo)

En el perfil `dev`, la aplicación crea estos usuarios, con la contraseña de `DEMO_PASSWORD` almacenada con BCrypt:

| Email | Rol | Datos asociados |
|---|---|---|
| `gerente@petsalud.cl` | GERENTE | — |
| `empleado@petsalud.cl` | EMPLEADO (veterinario) | Atiende citas y expedientes |
| `ana@cliente.cl` | CLIENTE | Mascota "Luna" (id 1) |
| `bruno@cliente.cl` | CLIENTE | Mascota "Michi" (id 2), una venta y una recompensa |

> No usar estas credenciales fuera de un entorno local de desarrollo.

## Flujo de autenticación JWT

```
1. POST /api/auth/login  {"email":"ana@cliente.cl","password":"..."}
   → 200 {"token":"eyJ...","tipo":"Bearer","expiraEnSegundos":3600,"usuarioId":3,"email":"...","rol":"CLIENTE"}
2. Cada solicitud protegida:  Authorization: Bearer <token>
3. JwtAuthenticationFilter valida la firma HS256, el emisor y la expiración; relee el usuario y su rol desde la BD
   → SecurityContext (sin sesión HTTP) → reglas por URL + @PreAuthorize
4. Token ausente, inválido, manipulado o expirado → 401 JSON · Rol o propiedad insuficiente → 403 JSON
```

- **Claims del token:** `sub` (email), `rol`, `iss`, `iat`, `exp` y `jti`. No incluye datos sensibles.
- **Bloqueo por intentos fallidos:** 5 por email o 20 por IP bloquean durante 15 minutos (respuesta **429** con `Retry-After`).

## Matriz de roles y endpoints

"propio" significa que el recurso debe pertenecer al usuario autenticado; si no, la respuesta es 403. Todos los endpoints, salvo login y registro, exigen un JWT válido.

| Endpoint | CLIENTE | EMPLEADO | GERENTE |
|---|---|---|---|
| `POST /api/auth/login` · `POST /api/usuarios/registro` (rol forzado a CLIENTE) | público | público | público |
| `GET /api/usuarios/{id}` | propio | ✔ | ✔ |
| `PUT /api/usuarios/{id}` | propio | propio | ✔ |
| `PUT /api/usuarios/{id}/rol` · `DELETE /api/usuarios/{id}` | ✘ | ✘ | ✔ |
| `GET /api/usuarios` | ✘ | ✔ | ✔ |
| `POST /api/mascotas/propietario/{id}` · `GET/PUT /api/mascotas/{id}` · `GET /api/mascotas/propietario/{id}` | propio | ✔ | ✔ |
| `DELETE /api/mascotas/{id}` | propio | ✘ | ✔ |
| `POST /api/citas` · `GET /api/citas/{id}` · `DELETE /api/citas/{id}` · `GET /api/citas/mascota/{id}` | propio | ✔ | ✔ |
| `PUT /api/citas/{id}/estado` · `GET /api/citas/veterinario/{id}` · `GET /api/citas/entre-fechas` | ✘ | ✔ | ✔ |
| `POST /api/expedientes` | ✘ | ✔ | ✔ |
| `GET /api/expedientes/{id}` · `GET /api/expedientes/mascota/{id}` | propio | ✔ | ✔ |
| `DELETE /api/expedientes/{id}` | ✘ | ✘ | ✔ |
| `GET /api/productos` · `GET /api/productos/{id}` · `GET /api/productos/categoria` | ✔ | ✔ | ✔ |
| `POST /api/productos` · `PUT /api/productos/{id}` · `GET /api/productos/stock-bajo` | ✘ | ✔ | ✔ |
| `DELETE /api/productos/{id}` | ✘ | ✘ | ✔ |
| `POST /api/ventas` · `GET /api/ventas/{id}` · `GET /api/ventas/usuario/{id}` | propio | ✔ | ✔ |
| `GET /api/ventas` (reporte) | ✘ | ✘ | ✔ |
| `POST /api/recompensas/usuario/{id}` | ✘ | ✔ | ✔ |
| `GET /api/recompensas/{id}` · `GET /api/recompensas/usuario/{id}` | propio | ✔ | ✔ |
| `DELETE /api/recompensas/{id}` | ✘ | ✘ | ✔ |
| `POST /api/notificaciones/usuario/{id}` | ✘ | ✔ | ✔ |
| `GET /api/notificaciones/{id}` · `/usuario/{id}` · `/usuario/{id}/no-leidas` | propio | ✔ | ✔ |
| `PUT /api/notificaciones/{id}/leida` · `DELETE /api/notificaciones/{id}` | propio | ✘ | ✔ |

Cada fila está verificada con pruebas automatizadas en `MatrizAutorizacionTest` (cada endpoint × 4 usuarios, más el caso sin token).

## Medidas de seguridad por amenaza

| Amenaza | Medidas |
|---|---|
| Acceso no autorizado / IDOR | JWT obligatorio; reglas por URL + `@PreAuthorize` en los 44 métodos (`denyAll` por defecto); verificación de propiedad (`@authz`); rol vigente releído desde la BD; DTOs sin datos sensibles |
| Escalada y asignación masiva | El registro no acepta `rol` ni `id` (siempre CLIENTE); solo el GERENTE asigna roles |
| Robo de credenciales / fuerza bruta | BCrypt (costo 12); política de contraseña; mensaje genérico; bloqueo por email/IP con 429 |
| Manipulación de token | HS256 con secreto ≥ 256 bits en variable de entorno; validación de firma, `iss` y `exp`; expiración de 60 minutos |
| SQL Injection | Solo consultas derivadas de Spring Data (parametrizadas); ids tipados; validación de tamaño y formato |
| XSS | `@TextoSeguro` (OWASP HTML Sanitizer) rechaza HTML/JS con 400; respuestas JSON; CSP `default-src 'none'`, `nosniff`, `X-Frame-Options: DENY` |
| CSRF | API stateless, sin sesión ni cookies de autenticación; JWT en la cabecera `Authorization`; CORS restringido |
| Fuga de información | Manejador global de errores sin trazas; `server.error.include-stacktrace=never`; `show-sql=false` |
| Falta de monitoreo | `logs/seguridad.log`: logins, bloqueos, tokens inválidos, 401/403 y cambios de rol, con email enmascarado y sin contraseñas ni tokens |
| Componentes sin soporte | Spring Boot 4.1.2 (soporte OSS vigente) |

## Estructura del proyecto

```
src/main/java/com/petsalud/backendpetsalud/
├── config/        DatosInicialesConfig (usuarios demo, perfiles dev/test)
├── controller/    AuthController + 8 controladores REST (DTOs y @PreAuthorize)
├── dto/           Records de entrada (validados) y salida (sin datos sensibles)
├── entity/        Entidades JPA y enum Rol (CLIENTE, EMPLEADO, GERENTE)
├── exception/     GlobalExceptionHandler, ErrorRespuesta y excepciones de dominio
├── repository/    Repositorios Spring Data (consultas derivadas parametrizadas)
├── security/      SecurityConfig, JwtUtil, JwtAuthenticationFilter, handlers 401/403,
│                  UsuarioDetails(Service), AutorizacionService (@authz), IntentosLoginService,
│                  AuditoriaSeguridad, Permisos
├── service/       Interfaces e implementaciones de negocio
└── validation/    @TextoSeguro (anti-XSS)
src/main/resources/  application.properties (sin secretos), logback-spring.xml
src/test/java/...    MatrizAutorizacionTest, AutenticacionJwtTest, AmenazasComunesTest,
                     MonitoreoSeguridadTest, LongitudCamposTest y pruebas unitarias
```

## Autor

Trabajo individual.

| | |
|---|---|
| **Estudiante** | María Ibaceta Jofre |
| **Carrera** | Analista Programador |
| **Asignatura** | Desarrollo Backend II (PBY2202) |
| **Profesor** | Marcelo Zepeda Aracena |
