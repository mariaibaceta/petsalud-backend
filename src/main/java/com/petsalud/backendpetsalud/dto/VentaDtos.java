package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class VentaDtos {

    private VentaDtos() {
    }

    public record ItemVenta(Long productoId, String nombre, BigDecimal precio) {
    }

    public record VentaResponse(Long id, LocalDateTime fecha, BigDecimal total, Long usuarioId, List<ItemVenta> productos) {
        public static VentaResponse de(Venta v) {
            List<ItemVenta> items = v.getProductos() == null ? List.of() : v.getProductos().stream()
                    .map(p -> new ItemVenta(p.getId(), p.getNombre(), p.getPrecio())).toList();
            return new VentaResponse(v.getId(), v.getFecha(), v.getTotal(),
                    v.getUsuario() != null ? v.getUsuario().getId() : null, items);
        }
    }
}
