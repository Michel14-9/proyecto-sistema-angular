package com.sistemaapolloAngular.sistema_apolloAngular.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "direcciones")
public class Direccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo; // casa, oficina, departamento, otro

    @Column(nullable = false, length = 500)
    private String direccion;

    @Column(length = 300)
    private String referencia;

    @Column(nullable = false)
    private String ciudad;

    private String telefono;

    private boolean predeterminada = false;

    private boolean facturacion = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Constructores
    public Direccion() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Direccion(String nombre, String tipo, String direccion, String ciudad, Usuario usuario) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.usuario = usuario;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public boolean isPredeterminada() { return predeterminada; }
    public void setPredeterminada(boolean predeterminada) { this.predeterminada = predeterminada; }

    public boolean isFacturacion() { return facturacion; }
    public void setFacturacion(boolean facturacion) { this.facturacion = facturacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    @Override
    public String toString() {
        return "Direccion{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", direccion='" + direccion + '\'' +
                ", referencia='" + referencia + '\'' +
                ", ciudad='" + ciudad + '\'' +
                ", telefono='" + telefono + '\'' +
                ", predeterminada=" + predeterminada +
                ", facturacion=" + facturacion +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}

