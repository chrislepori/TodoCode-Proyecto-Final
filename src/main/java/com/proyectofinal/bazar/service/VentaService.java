package com.proyectofinal.bazar.service;

import com.proyectofinal.bazar.dto.ProductoVentaDTO;
import com.proyectofinal.bazar.dto.VentaDTO;
import com.proyectofinal.bazar.exception.ApiException;
import com.proyectofinal.bazar.exception.MensajeError;
import com.proyectofinal.bazar.model.Cliente;
import com.proyectofinal.bazar.model.Producto;
import com.proyectofinal.bazar.model.Venta;
import com.proyectofinal.bazar.repository.ClienteRepository;
import com.proyectofinal.bazar.repository.ProductoRepository;
import com.proyectofinal.bazar.repository.VentaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepo;
    private final ProductoRepository productoRepo;
    private final ClienteRepository clienteRepo;

    public Venta createVenta(VentaDTO ventaDTO) {
        // Validar que haya productos en la venta
        if (noHayProductos(ventaDTO)) {
            throw new ApiException(MensajeError.VENTA_SIN_PRODUCTO);
        }

        // Buscar los productos en la BD y validar stock
        List<Producto> productos = obtenerProductosValidos(ventaDTO);

        // Validar que el cliente exista si se envió un ID
        Cliente cliente = validarCliente(ventaDTO);

        // Crear la venta con los datos validados
        Venta nuevaVenta = getVenta(productos, cliente);

        // Guardar la venta en la BD
        Venta ventaGuardada = ventaRepo.save(nuevaVenta);

        // Descontar stock y actualizar productos en la BD
        descontarProductos(productos);

        return ventaGuardada;
    }


    private boolean noHayProductos(VentaDTO ventaDTO) {
        return ventaDTO.getProductosIds() == null || ventaDTO.getProductosIds().isEmpty();
    }

    private List<Producto> obtenerProductosValidos(VentaDTO ventaDTO) {
        List<Producto> productos = new ArrayList<>();
        for (Long id : ventaDTO.getProductosIds()) {
            Producto producto = productoRepo.findById(id)
                    .orElseThrow(() -> new ApiException(MensajeError.PRODUCTO_NOT_FOUD));

            if (!producto.tieneStock()) {
                throw new ApiException(MensajeError.PRODUCT_NO_STOCK);
            }

            productos.add(producto);
        }
        return productos;
    }

    private Cliente validarCliente(VentaDTO ventaDTO) {
        Cliente cliente = clienteRepo.findById(ventaDTO.getClienteId())
                .orElseThrow(() -> new ApiException(MensajeError.CLIENTE_NOT_FOUND));

        return cliente;
    }

    private Venta getVenta(List<Producto> productos, Cliente cliente) {
        Venta nuevaVenta = new Venta();
        nuevaVenta.setFechaVenta(LocalDate.now());
        nuevaVenta.setProductos(productos);
        nuevaVenta.setCliente(cliente);
        nuevaVenta.obtenerMonto();
        return nuevaVenta;
    }

    private void descontarProductos(List<Producto> productos) {
        for (Producto p : productos) {
            p.descontarCantidad();
        }
        productoRepo.saveAll(productos);
    }

    public List<Venta> getVentas() {
        return ventaRepo.findAll();
    }


    public Venta findVenta(Long id) {
        return ventaRepo.findById(id).orElse(null);
    }

    public void deleteVenta(Long id) {
        Venta venta = ventaRepo.findById(id)
                .orElseThrow(() -> new ApiException(MensajeError.VENTA_NOT_FOUND));

        for (Producto p : venta.getProductos()) {
            p.aumentarCantidad();
        }

        // Guardar los productos actualizados en la base de datos
        productoRepo.saveAll(venta.getProductos());

        // Eliminar la venta de la base de datos
        ventaRepo.delete(venta);
    }


    public List<Producto> productosDeUnaVenta(Long id) {
        Venta venta = ventaRepo.findById(id)
                .orElseThrow(() -> new ApiException(MensajeError.VENTA_NOT_FOUND));
        return venta.getProductos();
    }


    public List<Venta> ventasPorDia(LocalDate fecha) {
        return ventaRepo.findByFechaVenta(fecha);


    }

    private ProductoVentaDTO convertirAVentaDTO(Venta venta) {
        return new ProductoVentaDTO(venta.getId(), venta.getTotal(), venta.getProductos().size(), venta.getCliente().getNombre(), venta.getCliente().getApellido());
    }


    public ProductoVentaDTO obtenerVentaMayor() {
        List<Venta> listaVentas = this.getVentas();
        Venta ventaMayor = listaVentas.stream()
                .max(Comparator.comparingDouble(Venta::obtenerMonto))
                .orElseThrow(() -> new ApiException(MensajeError.VENTA_NOT_FOUND));
        return convertirAVentaDTO(ventaMayor);


    }


}
