package com.proyecto.servicios.service.producto;

import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse;

import java.util.List;

public interface TransformProduct {

    List<GestoPagoProductXmlResponse.ProductoXmlItem> transformProductXML(String xmlRequest);

    List<com.proyecto.servicios.entity.gestopago.productos.Producto> transformProductEntities(List<GestoPagoProductXmlResponse.ProductoXmlItem> itemList);

    List<ProductoResponse> transformProductJSON(List<GestoPagoProductXmlResponse.ProductoXmlItem> itemList);
}

