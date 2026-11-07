package com.petsalud.backendpetsalud.entity;

/**
 * Roles exigidos por el caso Clínica PetSalud.
 * <ul>
 *   <li>CLIENTE: dueño de mascotas; solo accede a sus propios datos.</li>
 *   <li>EMPLEADO: personal de la clínica (incluye veterinarios); gestiona la operación diaria.</li>
 *   <li>GERENTE: administración; reportes, eliminaciones y asignación de roles.</li>
 * </ul>
 * Spring Security los expone como autoridades con prefijo {@code ROLE_} (p. ej. {@code ROLE_GERENTE}).
 */
public enum Rol {
    CLIENTE,
    EMPLEADO,
    GERENTE
}
