package com.sistemaapolloAngular.sistema_apolloAngular.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String canal;
    private String numero;
    private String estado;
    private LocalDateTime fecha;

    private Double total;
    private String observaciones;

    private Double subtotal;
    private Double costoEnvio;
    private Double descuento;

    private String metodoPago;
    private String tipoEntrega;
    private String direccionEntrega;
    private String instrucciones;

    private String numeroPedido;

    // ✅ NUEVOS CAMPOS
    @Column(name = "preference_id")
    private String preferenceId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "origen")
    private String origen; // ONLINE, PRESENCIAL

    @Column(name = "listo_para_cocina")
    private boolean listoParaCocina = false;

    @Column(name = "nombre_cliente")
    private String nombreCliente; // Para pedidos presenciales sin usuario

    @Column(name = "telefono_cliente")
    private String telefonoCliente; // Para pedidos presenciales sin usuario

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemPedido> items = new ArrayList<>();

    // CONSTRUCTOR
    public Pedido() {
        this.fecha = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = "PENDIENTE";
        this.items = new ArrayList<>();
        this.origen = "ONLINE";
        this.listoParaCocina = false;
    }

    public String getCodigo() {
        return (canal != null && numero != null) ? canal + "-" + numero : "N/A";
    }

    public LocalDateTime getFechaPedido() {
        return fecha;
    }

    public void agregarItem(ItemPedido item) {
        item.setPedido(this);
        this.items.add(item);
    }

    public void calcularTotales() {
        this.subtotal = items.stream()
                .mapToDouble(ItemPedido::getSubtotal)
                .sum();

        this.costoEnvio = "DELIVERY".equals(tipoEntrega) && subtotal < 50 ? 5.0 : 0.0;
        this.descuento = subtotal >= 80 ? 10.0 : 0.0;
        this.total = subtotal + costoEnvio - descuento;
    }

    // ===== Getters y Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Double getTotal() {
        if (total == null && subtotal != null) {
            return subtotal + (costoEnvio != null ? costoEnvio : 0) - (descuento != null ? descuento : 0);
        }
        return total != null ? total : 0.0;
    }
    public void setTotal(Double total) { this.total = total; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<ItemPedido> getItems() { return items; }
    public void setItems(List<ItemPedido> items) { this.items = items; }

    public Double getSubtotal() {
        if (subtotal == null && !items.isEmpty()) {
            return items.stream().mapToDouble(ItemPedido::getSubtotal).sum();
        }
        return subtotal != null ? subtotal : 0.0;
    }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getCostoEnvio() { return costoEnvio != null ? costoEnvio : 0.0; }
    public void setCostoEnvio(Double costoEnvio) { this.costoEnvio = costoEnvio; }

    public Double getDescuento() { return descuento != null ? descuento : 0.0; }
    public void setDescuento(Double descuento) { this.descuento = descuento; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }

    public String getNumeroPedido() {
        if (numeroPedido == null && id != null) {
            return "LR" + String.format("%06d", id);
        }
        return numeroPedido;
    }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    // ✅ GETTERS Y SETTERS NUEVOS
    public String getPreferenceId() { return preferenceId; }
    public void setPreferenceId(String preferenceId) { this.preferenceId = preferenceId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public boolean isListoParaCocina() { return listoParaCocina; }
    public void setListoParaCocina(boolean listoParaCocina) { this.listoParaCocina = listoParaCocina; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }
}