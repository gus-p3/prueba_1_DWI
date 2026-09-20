package com.proyecto.servicios.mapper.producto;


import com.proyecto.servicios.entity.gestopago.productos.Producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoPatchRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import org.mapstruct.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "id", ignore = true)
    Producto toEntity(ProductoRequest request);

    ProductoResponse toResponse(Producto entity);

    List<ProductoResponse> toResponseList(List<Producto> entities);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(ProductoRequest request, @MappingTarget Producto entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromPatch(ProductoPatchRequest request, @MappingTarget Producto entity);

    // Mapeos MapStruct de items XML a ProductoResponse (DTO)
    @Mapping(target = "id", ignore = true)
    ProductoResponse xmlItemToResponse(com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse.ProductoXmlItem xmlItem);

    List<ProductoResponse> xmlItemsToResponseList(List<com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse.ProductoXmlItem> xmlItems);

    // Mapeos MapStruct de items XML a Producto (Entidad JPA)
    @Mapping(target = "id", ignore = true)
    Producto xmlItemToEntity(com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse.ProductoXmlItem xmlItem);

    List<Producto> xmlItemsToEntityList(List<com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse.ProductoXmlItem> xmlItems);

    default List<ProductoResponse> filtrarYOrdenarPorTipoFront(List<ProductoResponse> jsonItems) {
        if (jsonItems == null || jsonItems.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(1);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        return jsonItems.stream()
                .filter(p -> p.getTipoFront() != null && p.getTipoFront() > 0)
                .sorted(Comparator.comparing(ProductoResponse::getTipoFront, Comparator.nullsLast(Comparator.naturalOrder())))
                .peek(p -> {
                    if (p.getId() == null) {
                        p.setId(index.getAndIncrement());
                    }
                    if (p.getCreatedAt() == null) {
                        p.setCreatedAt(now);
                    }
                })
                .collect(Collectors.toList());
    }
}

