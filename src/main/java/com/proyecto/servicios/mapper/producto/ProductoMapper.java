package com.proyecto.servicios.mapper.producto;


import com.proyecto.servicios.entity.gestopago.productos.Producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoPatchRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import org.mapstruct.*;

import java.util.List;

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
}