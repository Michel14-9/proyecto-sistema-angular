package tracking.apollo.backendtracking.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tracking_delivery")
public class TrackingDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "numero_pedido", nullable = false)
    private String numeroPedido;

    // READY → repartidor asignado pero no salió
    // ACTIVE → repartidor en camino, enviando GPS
    // COMPLETED → entregado
    // CANCELLED → cancelado
    @Column(nullable = false)
    private String estado = "READY";

    @Column(name = "lat_repartidor")
    private Double latRepartidor;

    @Column(name = "lng_repartidor")
    private Double lngRepartidor;

    @Column(name = "lat_cliente")
    private Double latCliente;

    @Column(name = "lng_cliente")
    private Double lngCliente;

    @Column(name = "lat_local")
    private Double latLocal;

    @Column(name = "lng_local")
    private Double lngLocal;

    @Column(name = "direccion_cliente", length = 500)
    private String direccionCliente;

    // ETA calculado con Distance Matrix o Haversine
    @Column(name = "eta_minutos")
    private Integer etaMinutos;

    @Column(name = "distancia_km")
    private Double distanciaKm;

    @Column(name = "nombre_repartidor", length = 200)
    private String nombreRepartidor;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;

    public TrackingDelivery() {
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaActualizacion = LocalDateTime.now();
    }

    // ─── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getLatRepartidor() { return latRepartidor; }
    public void setLatRepartidor(Double latRepartidor) { this.latRepartidor = latRepartidor; }
    public Double getLngRepartidor() { return lngRepartidor; }
    public void setLngRepartidor(Double lngRepartidor) { this.lngRepartidor = lngRepartidor; }
    public Double getLatCliente() { return latCliente; }
    public void setLatCliente(Double latCliente) { this.latCliente = latCliente; }
    public Double getLngCliente() { return lngCliente; }
    public void setLngCliente(Double lngCliente) { this.lngCliente = lngCliente; }
    public Double getLatLocal() { return latLocal; }
    public void setLatLocal(Double latLocal) { this.latLocal = latLocal; }
    public Double getLngLocal() { return lngLocal; }
    public void setLngLocal(Double lngLocal) { this.lngLocal = lngLocal; }
    public String getDireccionCliente() { return direccionCliente; }
    public void setDireccionCliente(String direccionCliente) { this.direccionCliente = direccionCliente; }
    public Integer getEtaMinutos() { return etaMinutos; }
    public void setEtaMinutos(Integer etaMinutos) { this.etaMinutos = etaMinutos; }
    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double distanciaKm) { this.distanciaKm = distanciaKm; }
    public String getNombreRepartidor() { return nombreRepartidor; }
    public void setNombreRepartidor(String nombreRepartidor) { this.nombreRepartidor = nombreRepartidor; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime f) { this.fechaCreacion = f; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime f) { this.fechaInicio = f; }
    public LocalDateTime getFechaCompletado() { return fechaCompletado; }
    public void setFechaCompletado(LocalDateTime f) { this.fechaCompletado = f; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime f) { this.ultimaActualizacion = f; }
}