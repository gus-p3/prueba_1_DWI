package com.proyecto.servicios.controller.producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import com.proyecto.servicios.service.producto.ProductoServiceRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/productos/redis")
@RequiredArgsConstructor
public class ProductoRedisController {

    private final ProductoServiceRedis productoServiceRedis;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getProductsRedis() {
        return ResponseEntity.ok(productoServiceRedis.getProductsRedis());
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<List<ProductoResponse>> sincronizarManual() {
        return ResponseEntity.ok(productoServiceRedis.sincronizarCatalogo());
    }
}
