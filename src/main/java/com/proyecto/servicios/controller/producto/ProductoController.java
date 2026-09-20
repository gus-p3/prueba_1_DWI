package com.proyecto.servicios.controller.producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoPatchRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import com.proyecto.servicios.service.producto.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // 1. POST /v1/productos -> Sincroniza los primeros 20 productos en la BD y retorna 201 Created sin cuerpo
    @PostMapping
    public ResponseEntity<Void> sincronizarProductos(UriComponentsBuilder uriComponentsBuilder) {
        productoService.sincronizarProductosDesdeGestoPago();

        URI location = uriComponentsBuilder
                .path("/v1/productos")
                .build()
                .toUri();

        return ResponseEntity.created(location).build();
    }

    // 2. GET /v1/productos -> Obtiene todos los productos guardados en la BD
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> obtenerTodosLosProductos() {
        return ResponseEntity.ok(productoService.obtenerTodosLosProductos());
    }

    // 3. GET /v1/productos/{id} -> Solo acepta IDs numéricos
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ProductoResponse> obtenerProductoPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
    }

    // 4. PUT /v1/productos/{id} -> Reemplazo completo de un producto por su ID
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ProductoResponse> reemplazarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.reemplazarProducto(id, request));
    }

    // 5. PATCH /v1/productos/{id} -> Actualización parcial de un producto por su ID
    @PatchMapping("/{id:\\d+}")
    public ResponseEntity<ProductoResponse> actualizarParcialProducto(
            @PathVariable Integer id,
            @RequestBody ProductoPatchRequest request) {
        return ResponseEntity.ok(productoService.actualizarParcialProducto(id, request));
    }

    // 6. DELETE /v1/productos/{id} -> Elimina un producto por su ID
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Integer id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    //7. GET /v1/productos/xml -> Obtener productos parceados en JSON
    @GetMapping("/xml")
    public ResponseEntity<List<ProductoResponse>> obtenerProductosXML(){
        return ResponseEntity.ok(productoService.obtenerProductosXML());
    }
}
