package com.sistemaapolloAngular.sistema_apolloAngular.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "item_pedido")
public class ItemPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreProducto;
    private Integer cantidad;
    private Double precio;
    private Double subtotal;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private ProductoFinal producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @JsonIgnore
    private Pedido pedido;


    public String getNombreProductoSeguro() {
        if (producto != null && producto.getNombre() != null) {
            return producto.getNombre();
        }
        return nombreProducto != null ? nombreProducto : "Producto no disponible";
    }


    public String getProductosParaTabla() {
        return getNombreProductoSeguro() + " (x" + getCantidad() + ")";
    }


    public Producto getProducto() {
        Producto producto = new Producto();
        producto.setNombre(getNombreProductoSeguro());
        producto.setPrecio(precio);
        return producto;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public Integer getCantidad() { return cantidad != null ? cantidad : 0; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Double getPrecio() { return precio != null ? precio : 0.0; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Double getSubtotal() {
        return subtotal != null ? subtotal : (getPrecio() * getCantidad());
    }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }


    public ProductoFinal getProductoFinal() { return producto; }
    public void setProductoFinal(ProductoFinal producto) { this.producto = producto; }
}


class Producto {
    private String nombre;
    private Double precio;

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
}
