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

    // ============================================================
    // ✅ MÉTODOS CORREGIDOS
    // ============================================================

    /**
     * Obtiene el nombre del producto de forma segura
     * Primero intenta del producto asociado, luego del campo guardado
     */
    public String getNombreProductoSeguro() {
        // ✅ Primero: del producto real
        if (this.producto != null && this.producto.getNombre() != null
                && !this.producto.getNombre().isEmpty()) {
            return this.producto.getNombre();
        }
        // ✅ Segundo: del campo guardado en el item
        if (this.nombreProducto != null && !this.nombreProducto.isEmpty()) {
            return this.nombreProducto;
        }
        // ✅ Último recurso
        return "Producto no disponible";
    }

    /**
     * Para mostrar en tablas con formato
     */
    public String getProductosParaTabla() {
        return getNombreProductoSeguro() + " (x" + getCantidad() + ")";
    }

    /**
     * ✅ CORREGIDO: Este método ahora devuelve el ProductoFinal real
     * ELIMINÉ la creación de un Producto ficticio
     */
    public ProductoFinal getProducto() {
        return this.producto;
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

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