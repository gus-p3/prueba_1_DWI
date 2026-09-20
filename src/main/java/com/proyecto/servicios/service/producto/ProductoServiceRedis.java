package com.proyecto.servicios.service.producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;

import java.util.List;

public interface ProductoServiceRedis {
    List<ProductoResponse> getProductsRedis();
    List<ProductoResponse> sincronizarCatalogo();
}
