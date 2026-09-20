package com.proyecto.servicios.mapper.producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductoMapperTest {

    private ProductoMapper productoMapper;

    @BeforeEach
    void setUp() {
        productoMapper = Mappers.getMapper(ProductoMapper.class);
    }

    @Test
    @DisplayName("filtrarYOrdenarPorTipoFront - Filtra correctamente items con tipoFront nulo o <= 0")
    void filtrarYOrdenarPorTipoFront_FiltraInvalidos() {
        ProductoResponse p1 = new ProductoResponse();
        p1.setProducto("Producto 1");
        p1.setTipoFront(1);

        ProductoResponse pNull = new ProductoResponse();
        pNull.setProducto("Producto Nulo");
        pNull.setTipoFront(null);

        ProductoResponse pCero = new ProductoResponse();
        pCero.setProducto("Producto Cero");
        pCero.setTipoFront(0);

        ProductoResponse pNegativo = new ProductoResponse();
        pNegativo.setProducto("Producto Negativo");
        pNegativo.setTipoFront(-2);

        List<ProductoResponse> resultado = productoMapper.filtrarYOrdenarPorTipoFront(
                Arrays.asList(p1, pNull, pCero, pNegativo)
        );

        assertEquals(1, resultado.size());
        assertEquals("Producto 1", resultado.get(0).getProducto());
    }

    @Test
    @DisplayName("filtrarYOrdenarPorTipoFront - Ordena ascendentemente por tipoFront")
    void filtrarYOrdenarPorTipoFront_OrdenaAscendente() {
        ProductoResponse p3 = new ProductoResponse();
        p3.setProducto("Tipo 3");
        p3.setTipoFront(3);

        ProductoResponse p1 = new ProductoResponse();
        p1.setProducto("Tipo 1");
        p1.setTipoFront(1);

        ProductoResponse p2 = new ProductoResponse();
        p2.setProducto("Tipo 2");
        p2.setTipoFront(2);

        List<ProductoResponse> resultado = productoMapper.filtrarYOrdenarPorTipoFront(
                Arrays.asList(p3, p1, p2)
        );

        assertEquals(3, resultado.size());
        assertEquals(1, resultado.get(0).getTipoFront());
        assertEquals(2, resultado.get(1).getTipoFront());
        assertEquals(3, resultado.get(2).getTipoFront());
    }

    @Test
    @DisplayName("filtrarYOrdenarPorTipoFront - Respeta ID existente y asigna correlativo solo si es nulo")
    void filtrarYOrdenarPorTipoFront_ManejoDeId() {
        ProductoResponse conId = new ProductoResponse();
        conId.setId(999);
        conId.setProducto("Con ID DB");
        conId.setTipoFront(1);

        ProductoResponse sinId = new ProductoResponse();
        sinId.setId(null);
        sinId.setProducto("Sin ID XML");
        sinId.setTipoFront(2);

        List<ProductoResponse> resultado = productoMapper.filtrarYOrdenarPorTipoFront(
                Arrays.asList(conId, sinId)
        );

        assertEquals(2, resultado.size());
        assertEquals(999, resultado.get(0).getId(), "Debe conservar el ID existente de la BD");
        assertNotNull(resultado.get(1).getId(), "Debe asignar un ID correlativo si era null");
    }

    @Test
    @DisplayName("filtrarYOrdenarPorTipoFront - Asigna createdAt si es nulo y respeta el existente")
    void filtrarYOrdenarPorTipoFront_CreatedAt() {
        LocalDateTime fechaExistente = LocalDateTime.of(2025, 1, 1, 10, 0);

        ProductoResponse conFecha = new ProductoResponse();
        conFecha.setTipoFront(1);
        conFecha.setCreatedAt(fechaExistente);

        ProductoResponse sinFecha = new ProductoResponse();
        sinFecha.setTipoFront(2);
        sinFecha.setCreatedAt(null);

        List<ProductoResponse> resultado = productoMapper.filtrarYOrdenarPorTipoFront(
                Arrays.asList(conFecha, sinFecha)
        );

        assertEquals(fechaExistente, resultado.get(0).getCreatedAt());
        assertNotNull(resultado.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("filtrarYOrdenarPorTipoFront - Retorna lista vacía ante entrada nula o vacía")
    void filtrarYOrdenarPorTipoFront_ListaVaciaONula() {
        assertTrue(productoMapper.filtrarYOrdenarPorTipoFront(null).isEmpty());
        assertTrue(productoMapper.filtrarYOrdenarPorTipoFront(Collections.emptyList()).isEmpty());
    }
}
