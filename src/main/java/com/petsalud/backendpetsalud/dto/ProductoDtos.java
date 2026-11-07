package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ProductoDtos {

    private ProductoDtos() {
    }

    public record ProductoRequest(
            @NotBlank @Size(max = 100) @TextoSeguro String nombre,
            @Size(max = 300) @TextoSeguro String descripcion,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal precio,
            @NotNull @PositiveOrZero Integer stock,
            @NotBlank @Size(max = 50) @TextoSeguro String categoria) {

        public Producto aEntidad() {
            return Producto.builder().nombre(nombre).descripcion(descripcion).precio(precio).stock(stock)
                    .categoria(categoria).build();
        }
    }

    public record ProductoResponse(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock,
                                   String categoria) {
        public static ProductoResponse de(Producto p) {
            return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getStock(),
                    p.getCategoria());
        }
    }
}
