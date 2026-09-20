package com.proyecto.servicios.service.producto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.productos.Producto;
import com.proyecto.servicios.mapper.producto.ProductoMapper;
import com.proyecto.servicios.model.gestopago.producto.ProductoResponse;
import com.proyecto.servicios.model.gestopago.xml.GestoPagoProductXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.repositorys.gestopago.producto.ProductoRepository;
import com.proyecto.servicios.service.Impl.producto.ProductServiceRedisImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceRedisImplTest {

    private static final String REDIS_CATALOGO_KEY = "catalogo:productos:gestopago";

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoAuthClient gestoPagoAuth;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private TransformProduct transformProduct;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductServiceRedisImpl productServiceRedis;

    private Producto mockProducto;
    private ProductoResponse mockProductoResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productServiceRedis, "idDistribuidor", 83);
        ReflectionTestUtils.setField(productServiceRedis, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(productServiceRedis, "password", "12345678");

        mockProducto = new Producto();
        mockProducto.setId(1);
        mockProducto.setProducto("TELCEL $100");
        mockProducto.setTipoFront(1);

        mockProductoResponse = new ProductoResponse();
        mockProductoResponse.setId(1);
        mockProductoResponse.setProducto("TELCEL $100");
        mockProductoResponse.setTipoFront(1);
    }

    @Test
    @DisplayName("getProductsRedis - Recupera de Redis y aplica filtrarYOrdenarPorTipoFront")
    void getProductsRedis_DesdeRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_CATALOGO_KEY)).thenReturn("cached_json_data");

        List<ProductoResponse> cacheList = List.of(mockProductoResponse);
        when(objectMapper.convertValue(eq("cached_json_data"), any(TypeReference.class)))
                .thenReturn(cacheList);
        when(productoMapper.filtrarYOrdenarPorTipoFront(cacheList))
                .thenReturn(cacheList);

        List<ProductoResponse> resultado = productServiceRedis.getProductsRedis();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("TELCEL $100", resultado.get(0).getProducto());

        verify(productoMapper, times(1)).filtrarYOrdenarPorTipoFront(cacheList);
        verify(productoRepository, never()).findAll();
    }

    @Test
    @DisplayName("getProductsRedis - Si Redis está vacío o falla, recupera de Postgres y aplica filtrarYOrdenarPorTipoFront")
    void getProductsRedis_DesdePostgres() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_CATALOGO_KEY)).thenReturn(null);

        List<Producto> enDb = List.of(mockProducto);
        List<ProductoResponse> responses = List.of(mockProductoResponse);

        when(productoRepository.findAll()).thenReturn(enDb);
        when(productoMapper.toResponseList(enDb)).thenReturn(responses);
        when(productoMapper.filtrarYOrdenarPorTipoFront(responses)).thenReturn(responses);

        List<ProductoResponse> resultado = productServiceRedis.getProductsRedis();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("TELCEL $100", resultado.get(0).getProducto());

        verify(productoRepository, times(1)).findAll();
        verify(productoMapper, times(1)).toResponseList(enDb);
        verify(productoMapper, times(1)).filtrarYOrdenarPorTipoFront(responses);
    }

    @Test
    @DisplayName("getProductsRedis - Si Redis y Postgres están vacíos, consume API externa y aplica filtrarYOrdenarPorTipoFront")
    void getProductsRedis_DesdeApiExterna() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_CATALOGO_KEY)).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(Collections.emptyList());

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token_123");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(anyInt(), anyString()))
                .thenReturn(Optional.of(token));

        String xmlMock = "<RESPONSE></RESPONSE>";
        when(gestoPagoProductClient.getProductListXml(anyString())).thenReturn(xmlMock);

        GestoPagoProductXmlResponse.ProductoXmlItem xmlItem = new GestoPagoProductXmlResponse.ProductoXmlItem();
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = List.of(xmlItem);
        when(transformProduct.transformProductXML(xmlMock)).thenReturn(items);

        List<ProductoResponse> itemsJson = List.of(mockProductoResponse);
        when(transformProduct.transformProductJSON(items)).thenReturn(itemsJson);
        when(productoMapper.filtrarYOrdenarPorTipoFront(itemsJson)).thenReturn(itemsJson);

        List<ProductoResponse> resultado = productServiceRedis.getProductsRedis();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(productoMapper, times(1)).filtrarYOrdenarPorTipoFront(itemsJson);
        verify(valueOperations, times(1)).set(eq(REDIS_CATALOGO_KEY), eq(itemsJson));
    }

    @Test
    @DisplayName("sincronizarCatalogo - Limpia Redis y Postgres y vuelve a sincronizar")
    void sincronizarCatalogo_Exito() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token_123");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(anyInt(), anyString()))
                .thenReturn(Optional.of(token));

        String xmlMock = "<RESPONSE></RESPONSE>";
        when(gestoPagoProductClient.getProductListXml(anyString())).thenReturn(xmlMock);

        GestoPagoProductXmlResponse.ProductoXmlItem xmlItem = new GestoPagoProductXmlResponse.ProductoXmlItem();
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = List.of(xmlItem);
        when(transformProduct.transformProductXML(xmlMock)).thenReturn(items);

        List<ProductoResponse> itemsJson = List.of(mockProductoResponse);
        when(transformProduct.transformProductJSON(items)).thenReturn(itemsJson);
        when(productoMapper.filtrarYOrdenarPorTipoFront(itemsJson)).thenReturn(itemsJson);

        productServiceRedis.sincronizarCatalogo();

        verify(redisTemplate, times(1)).delete(REDIS_CATALOGO_KEY);
        verify(productoRepository, times(1)).deleteAll();
        verify(productoMapper, times(1)).filtrarYOrdenarPorTipoFront(itemsJson);
    }
}
