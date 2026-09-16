package com.proyecto.servicios.service.Impl.producto;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.productos.Producto;
import com.proyecto.servicios.mapper.producto.ProductoMapper;
import com.proyecto.servicios.model.gestopago.producto.ProductoPatchRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.repositorys.gestopago.producto.ProductoRepository;
import com.proyecto.servicios.service.producto.ProductoService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenRepository tokenRepository;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    // 1. POST /v1/productos -> Consume la API XML de GestoPago, mapea los primeros 20 productos y los guarda en BD
    @Override
    @Transactional
    public void sincronizarProductosDesdeGestoPago() {
        log.info("Obteniendo token activo para consumir getProductList.do");
        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .orElseThrow(() -> new RuntimeException("No se encontró token activo de GestoPago. Ejecute o espere la autenticación inicial."));

        String bearerHeader = "Bearer " + tokenEntity.getToken();
        log.info("Llamando a la API XML getProductList.do de GestoPago...");
        String xmlResponse = gestoPagoProductClient.getProductListXml(bearerHeader);

        List<Producto> productosAGuardar = new ArrayList<>();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GestoPagoProductXmlResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmlResponse);
            GestoPagoProductXmlResponse parsedXml = (GestoPagoProductXmlResponse) unmarshaller.unmarshal(reader);

            if (parsedXml.getProductos() == null || parsedXml.getProductos().isEmpty()) {
                log.warn("La respuesta XML de GestoPago no contiene productos.");
                return;
            }

            // Mapear los primeros 20 productos para pruebas
            List<GestoPagoProductXmlResponse.ProductoXmlItem> items20 = parsedXml.getProductos()
                    .stream()
                    .limit(20)
                    .collect(Collectors.toList());

            log.info("Mapeando y guardando los primeros {} productos en la BD...", items20.size());

            for (GestoPagoProductXmlResponse.ProductoXmlItem item : items20) {
                Producto p = new Producto();
                p.setProducto(item.getProducto());
                p.setServicio(item.getServicio());
                p.setIdServicio(item.getIdServicio() != null ? item.getIdServicio() : 0);
                p.setIdProducto(item.getIdProducto() != null ? item.getIdProducto() : 0);
                p.setIdCatTipoServicio(item.getIdCatTipoServicio() != null ? item.getIdCatTipoServicio() : 0);
                p.setTipoFront(item.getTipoFront() != null ? item.getTipoFront() : 0);
                p.setHasDigitoVerificador(Boolean.TRUE.equals(item.getHasDigitoVerificador()));
                p.setTipoReferencia(item.getTipoReferencia());
                p.setPrecio(item.getPrecio());
                p.setShowAyuda(Boolean.TRUE.equals(item.getShowAyuda()));

                productosAGuardar.add(p);
            }

        } catch (Exception e) {
            log.error("Error al procesar el XML de getProductList.do: {}", e.getMessage(), e);
            throw new RuntimeException("Error parseando la respuesta XML de GestoPago: " + e.getMessage(), e);
        }

        List<Producto> guardados = productoRepository.saveAll(productosAGuardar);
        log.info("Se guardaron exitosamente {} productos en la BD", guardados.size());
    }

    // 2. GET /v1/productos
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerTodosLosProductos() {
        log.info("Consultando todos los productos de la BD");
        List<Producto> productos = productoRepository.findAll();
        return productoMapper.toResponseList(productos);
    }

    // 3. GET /v1/productos/{id}
    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(Integer id) {
        log.info("Buscando producto por ID: {}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return productoMapper.toResponse(producto);
    }

    // 4. PUT /v1/productos/{id} (Reemplazo completo)
    @Override
    @Transactional
    public ProductoResponse reemplazarProducto(Integer id, ProductoRequest request) {
        log.info("Reemplazando completo producto con ID: {}", id);
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        productoMapper.updateEntityFromRequest(request, existente);
        Producto actualizado = productoRepository.save(existente);
        return productoMapper.toResponse(actualizado);
    }

    // 5. PATCH /v1/productos/{id} (Actualización parcial)
    @Override
    @Transactional
    public ProductoResponse actualizarParcialProducto(Integer id, ProductoPatchRequest request) {
        log.info("Actualizando parcialmente producto con ID: {}", id);
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        productoMapper.updateEntityFromPatch(request, existente);
        Producto actualizado = productoRepository.save(existente);
        return productoMapper.toResponse(actualizado);
    }

    // 6. DELETE /v1/productos/{id}
    @Override
    @Transactional
    public void eliminarProducto(Integer id) {
        log.info("Eliminando producto con ID: {}", id);
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        productoRepository.deleteById(id);
    }


    @Override
    public List<ProductoResponse> obtenerProductosXML() {
        log.info("Obteniendo token activo para consumir getProductList.do");
        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .orElseThrow(() -> new RuntimeException("No se encontró token activo de GestoPago. Ejecute o espere la autenticación inicial."));

        String bearerHeader = "Bearer " + tokenEntity.getToken();
        log.info("Llamando a la API XML getProductList.do de GestoPago...");
        String xmlResponse = gestoPagoProductClient.getProductListXml(bearerHeader);

        List<ProductoResponse> response = new ArrayList<>();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GestoPagoProductXmlResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            if (xmlResponse == null || xmlResponse.isBlank()) {
                throw new RuntimeException("La respuesta XML recibida de GestoPago es nula o vacía. Verifique el token de autenticación o la API Key.");
            }

            StringReader reader = new StringReader(xmlResponse);
            GestoPagoProductXmlResponse parsedXml = (GestoPagoProductXmlResponse) unmarshaller.unmarshal(reader);

            if (parsedXml.getProductos() == null || parsedXml.getProductos().isEmpty()) {
                log.warn("La respuesta XML de GestoPago no contiene productos.");
                return response;
            }

            // Mapear los primeros 20 productos para pruebas
            List<GestoPagoProductXmlResponse.ProductoXmlItem> items20 = parsedXml.getProductos()
                    .stream()
                    .limit(20)
                    .collect(Collectors.toList());

            int i = 1;

            for (GestoPagoProductXmlResponse.ProductoXmlItem item : items20) {
                ProductoResponse p = new ProductoResponse();
                p.setId(i ++);
                p.setProducto(item.getProducto());
                p.setServicio(item.getServicio());
                p.setIdServicio(item.getIdServicio() != null ? item.getIdServicio() : 0);
                p.setIdProducto(item.getIdProducto() != null ? item.getIdProducto() : 0);
                p.setIdCatTipoServicio(item.getIdCatTipoServicio() != null ? item.getIdCatTipoServicio() : 0);
                p.setTipoFront(item.getTipoFront() != null ? item.getTipoFront() : 0);
                p.setHasDigitoVerificador(Boolean.TRUE.equals(item.getHasDigitoVerificador()));
                p.setTipoReferencia(item.getTipoReferencia());
                p.setPrecio(item.getPrecio());
                p.setShowAyuda(Boolean.TRUE.equals(item.getShowAyuda()));

                response.add(p);
            }

        } catch (Exception e) {
            log.error("Error al procesar el XML de getProductList.do: {}", e.getMessage(), e);
            throw new RuntimeException("Error parseando la respuesta XML de GestoPago: " + e.getMessage(), e);
        }

        return response;
    }
}