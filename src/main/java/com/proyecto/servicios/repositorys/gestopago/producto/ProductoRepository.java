package com.proyecto.servicios.repositorys.gestopago.producto;

import com.proyecto.servicios.entity.gestopago.productos.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
}