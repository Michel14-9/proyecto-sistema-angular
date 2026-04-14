package com.sistemaapolloAngular.sistema_apolloAngular.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favoritos")
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoFinal producto;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // Constructores
    public Favorito() {
        this.fechaAgregado = LocalDateTime.now();
        this.activo = true;
    }

    public Favorito(Usuario usuario, ProductoFinal producto) {
        this();
        this.usuario = usuario;
        this.producto = producto;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public ProductoFinal getProducto() {
        return producto;
    }

    public void setProducto(ProductoFinal producto) {
        this.producto = producto;
    }

    public LocalDateTime getFechaAgregado() {
        return fechaAgregado;
    }

    public void setFechaAgregado(LocalDateTime fechaAgregado) {
        this.fechaAgregado = fechaAgregado;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // Métodos de utilidad
    @Override
    public String toString() {
        return "Favorito{" +
                "id=" + id +
                ", usuario=" + (usuario != null ? usuario.getUsername() : "null") +
                ", producto=" + (producto != null ? producto.getNombre() : "null") +
                ", fechaAgregado=" + fechaAgregado +
                ", activo=" + activo +
                '}';
    }
}
