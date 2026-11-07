package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.ProductoDtos.ProductoRequest;
import com.petsalud.backendpetsalud.dto.ProductoDtos.ProductoResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.ProductoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @PostMapping
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<ProductoResponse> agregarProducto(@Valid @RequestBody ProductoRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductoResponse.de(productoService.agregarProducto(solicitud.aEntidad())));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    public ResponseEntity<ProductoResponse> obtenerProductoPorId(@PathVariable Long id) {
        return productoService.obtenerProductoPorId(id)
                .map(p -> ResponseEntity.ok(ProductoResponse.de(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<ProductoResponse> actualizarProducto(@PathVariable Long id,
                                                               @Valid @RequestBody ProductoRequest solicitud) {
        return ResponseEntity.ok(ProductoResponse.de(productoService.actualizarProducto(id, solicitud.aEntidad())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE)
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    public ResponseEntity<List<ProductoResponse>> listarProductos() {
        return ResponseEntity.ok(productoService.listarProductos().stream().map(ProductoResponse::de).toList());
    }

    /** El valor se usa en una consulta derivada de Spring Data (parametrizada): no es concatenado en SQL. */
    @GetMapping("/categoria")
    @PreAuthorize(Permisos.AUTENTICADO)
    public ResponseEntity<List<ProductoResponse>> listarProductosPorCategoria(
            @RequestParam @NotBlank @Size(max = 50) String categoria) {
        return ResponseEntity.ok(productoService.listarProductosPorCategoria(categoria).stream()
                .map(ProductoResponse::de).toList());
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<List<ProductoResponse>> listarProductosConStockBajo(
            @RequestParam @Min(0) @Max(100000) Integer cantidad) {
        return ResponseEntity.ok(productoService.listarProductosConStockBajo(cantidad).stream()
                .map(ProductoResponse::de).toList());
    }
}
