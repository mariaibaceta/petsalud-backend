package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.exception.ReglaNegocioException;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VentaServiceImpl implements VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Transaccional: si un producto no tiene stock, no se descuenta stock de ningún otro (atomicidad).
     * Un ID repetido en la lista significa varias unidades del mismo producto.
     */
    @Override
    @Transactional
    public Venta realizarVenta(Long usuarioId, List<Long> productosIds) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Map<Long, Long> cantidades = productosIds.stream()
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        Map<Long, Producto> productos = productoRepository.findAllById(cantidades.keySet()).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        if (productos.size() != cantidades.size()) {
            throw new RecursoNoEncontradoException("Algunos productos no fueron encontrados");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<Producto> items = new ArrayList<>();
        for (Map.Entry<Long, Long> linea : cantidades.entrySet()) {
            Producto producto = productos.get(linea.getKey());
            int cantidad = linea.getValue().intValue();
            int stock = producto.getStock() == null ? 0 : producto.getStock();
            if (stock < cantidad) {
                throw new ReglaNegocioException("Stock insuficiente para el producto " + producto.getNombre());
            }
            producto.setStock(stock - cantidad);
            total = total.add(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            for (int i = 0; i < cantidad; i++) {
                items.add(producto);
            }
        }

        Venta venta = Venta.builder()
                .usuario(usuario)
                .productos(items)
                .total(total)
                .fecha(LocalDateTime.now())
                .build();

        return ventaRepository.save(venta);
    }

    @Override
    public Optional<Venta> obtenerVentaPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Override
    public List<Venta> listarVentasPorUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Venta> listarTodasLasVentas() {
        return ventaRepository.findAll();
    }
}
