package com.proyecto.servicios.service.producto;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.productos.Producto;
import com.proyecto.servicios.mapper.producto.ProductoMapper;
import com.proyecto.servicios.model.gestopago.producto.ProductoPatchRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoRequest;
import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.repositorys.gestopago.producto.ProductoRepository;
import com.proyecto.servicios.service.Impl.producto.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoMapper productoMapper;

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private GestoPagoToken mockToken;
    private Producto mockProducto;
    private ProductoResponse mockProductoResponse;

    private static final String SAMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<RESPONSE>" +
            "<PRODUCTOS>" +
            "<producto producto=\"TELCEL $100\" servicio=\"RECARGA TELCEL\" idServicio=\"1\" idProducto=\"101\" idCatTipoServicio=\"2\" tipoFront=\"1\" hasDigitoVerificador=\"true\" tipoReferencia=\"TELEFONO\" precio=\"100.00\" showAyuda=\"false\"/>" +
            "</PRODUCTOS>" +
            "</RESPONSE>";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "GPS83-TPV-17");

        mockToken = new GestoPagoToken();
        mockToken.setId(1);
        mockToken.setIdDistribuidor(83);
        mockToken.setCodigoDispositivo("GPS83-TPV-17");
        mockToken.setToken("mock_bearer_token_xyz");

        mockProducto = new Producto();
        mockProducto.setId(1);
        mockProducto.setProducto("TELCEL $100");
        mockProducto.setServicio("RECARGA TELCEL");
        mockProducto.setPrecio("100.00");

        mockProductoResponse = new ProductoResponse();
        mockProductoResponse.setId(1);
        mockProductoResponse.setProducto("TELCEL $100");
        mockProductoResponse.setServicio("RECARGA TELCEL");
        mockProductoResponse.setPrecio("100.00");
    }

    @Test
    @DisplayName("sincronizarProductosDesdeGestoPago - Éxito")
    void sincronizarProductosDesdeGestoPago_Exito() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(mockToken));
        when(gestoPagoProductClient.getProductListXml("Bearer mock_bearer_token_xyz"))
                .thenReturn(SAMPLE_XML);
        when(productoRepository.saveAll(anyList()))
                .thenReturn(List.of(mockProducto));

        assertDoesNotThrow(() -> productoService.sincronizarProductosDesdeGestoPago());

        verify(tokenRepository, times(1)).findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17");
        verify(gestoPagoProductClient, times(1)).getProductListXml("Bearer mock_bearer_token_xyz");
        verify(productoRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("sincronizarProductosDesdeGestoPago - Token No Encontrado Lanza Excepción")
    void sincronizarProductosDesdeGestoPago_TokenNoEncontrado() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productoService.sincronizarProductosDesdeGestoPago());

        assertTrue(exception.getMessage().contains("No se encontró token activo"));
        verify(gestoPagoProductClient, never()).getProductListXml(anyString());
    }

    @Test
    @DisplayName("obtenerTodosLosProductos - Éxito")
    void obtenerTodosLosProductos_Exito() {
        when(productoRepository.findAll()).thenReturn(List.of(mockProducto));
        when(productoMapper.toResponseList(anyList())).thenReturn(List.of(mockProductoResponse));

        List<ProductoResponse> resultado = productoService.obtenerTodosLosProductos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("TELCEL $100", resultado.get(0).getProducto());
        verify(productoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenerProductoPorId - Éxito")
    void obtenerProductoPorId_Exito() {
        when(productoRepository.findById(1)).thenReturn(Optional.of(mockProducto));
        when(productoMapper.toResponse(mockProducto)).thenReturn(mockProductoResponse);

        ProductoResponse resultado = productoService.obtenerProductoPorId(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("TELCEL $100", resultado.getProducto());
    }

    @Test
    @DisplayName("obtenerProductoPorId - No Encontrado Lanza Excepción")
    void obtenerProductoPorId_NoEncontrado() {
        when(productoRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productoService.obtenerProductoPorId(999));

        assertTrue(exception.getMessage().contains("Producto no encontrado con id: 999"));
    }

    @Test
    @DisplayName("reemplazarProducto - Éxito")
    void reemplazarProducto_Exito() {
        ProductoRequest request = new ProductoRequest();
        request.setProducto("MOVISTAR $50");

        when(productoRepository.findById(1)).thenReturn(Optional.of(mockProducto));
        doNothing().when(productoMapper).updateEntityFromRequest(eq(request), eq(mockProducto));
        when(productoRepository.save(mockProducto)).thenReturn(mockProducto);
        when(productoMapper.toResponse(mockProducto)).thenReturn(mockProductoResponse);

        ProductoResponse resultado = productoService.reemplazarProducto(1, request);

        assertNotNull(resultado);
        verify(productoRepository, times(1)).save(mockProducto);
    }

    @Test
    @DisplayName("actualizarParcialProducto - Éxito")
    void actualizarParcialProducto_Exito() {
        ProductoPatchRequest patchRequest = new ProductoPatchRequest();
        patchRequest.setPrecio("150.00");

        when(productoRepository.findById(1)).thenReturn(Optional.of(mockProducto));
        doNothing().when(productoMapper).updateEntityFromPatch(eq(patchRequest), eq(mockProducto));
        when(productoRepository.save(mockProducto)).thenReturn(mockProducto);
        when(productoMapper.toResponse(mockProducto)).thenReturn(mockProductoResponse);

        ProductoResponse resultado = productoService.actualizarParcialProducto(1, patchRequest);

        assertNotNull(resultado);
        verify(productoRepository, times(1)).save(mockProducto);
    }

    @Test
    @DisplayName("eliminarProducto - Éxito")
    void eliminarProducto_Exito() {
        when(productoRepository.existsById(1)).thenReturn(true);
        doNothing().when(productoRepository).deleteById(1);

        assertDoesNotThrow(() -> productoService.eliminarProducto(1));

        verify(productoRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("eliminarProducto - No Encontrado Lanza Excepción")
    void eliminarProducto_NoEncontrado() {
        when(productoRepository.existsById(999)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productoService.eliminarProducto(999));

        assertTrue(exception.getMessage().contains("Producto no encontrado con id: 999"));
        verify(productoRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("obtenerProductosXML - Éxito")
    void obtenerProductosXML_Exito() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(mockToken));
        when(gestoPagoProductClient.getProductListXml("Bearer mock_bearer_token_xyz"))
                .thenReturn(SAMPLE_XML);

        List<ProductoResponse> resultado = productoService.obtenerProductosXML();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("TELCEL $100", resultado.get(0).getProducto());
    }

    @Test
    @DisplayName("obtenerProductosXML - XML Vacío Lanza Excepción")
    void obtenerProductosXML_XmlVacio() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(mockToken));
        when(gestoPagoProductClient.getProductListXml("Bearer mock_bearer_token_xyz"))
                .thenReturn("");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productoService.obtenerProductosXML());

        assertTrue(exception.getMessage().contains("respuesta XML recibida de GestoPago es nula o vacía"));
    }
}
