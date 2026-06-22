// CarritoItemDTO.java
package com.sistemaapolloAngular.sistema_apolloAngular.dto;

import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;

public class CarritoItemDTO {
    private Long id;
    private Long productoId;
    private String nombre;
    private String imagenUrl;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    public CarritoItemDTO() {}

    public CarritoItemDTO(CarritoItem item) {
        this.id = item.getId();
        this.cantidad = item.getCantidad();
        this.precioUnitario = item.getPrecioUnitario();
        this.subtotal = this.precioUnitario * this.cantidad;


        if (item.getProducto() != null) {
            this.productoId = item.getProducto().getId(); // ← Cambiado a getId()
            this.nombre = item.getProducto().getNombre();
            this.imagenUrl = item.getProducto().getImagenUrl();
        }
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
}