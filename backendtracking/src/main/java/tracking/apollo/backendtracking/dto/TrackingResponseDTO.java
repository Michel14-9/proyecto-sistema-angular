package tracking.apollo.backendtracking.dto;

public class TrackingResponseDTO {
    private Long id;
    private String numeroPedido;
    private String estado;
    private Double latRepartidor;
    private Double lngRepartidor;
    private Double latCliente;
    private Double lngCliente;
    private Double latLocal;
    private Double lngLocal;
    private String direccionCliente;
    private Integer etaMinutos;
    private Double distanciaKm;
    private String nombreRepartidor;
    private String mensaje;
    // URL de navegación para el repartidor (Google Maps sin API key)
    private String urlNavegacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String n) { this.numeroPedido = n; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getLatRepartidor() { return latRepartidor; }
    public void setLatRepartidor(Double l) { this.latRepartidor = l; }
    public Double getLngRepartidor() { return lngRepartidor; }
    public void setLngRepartidor(Double l) { this.lngRepartidor = l; }
    public Double getLatCliente() { return latCliente; }
    public void setLatCliente(Double l) { this.latCliente = l; }
    public Double getLngCliente() { return lngCliente; }
    public void setLngCliente(Double l) { this.lngCliente = l; }
    public Double getLatLocal() { return latLocal; }
    public void setLatLocal(Double l) { this.latLocal = l; }
    public Double getLngLocal() { return lngLocal; }
    public void setLngLocal(Double l) { this.lngLocal = l; }
    public String getDireccionCliente() { return direccionCliente; }
    public void setDireccionCliente(String d) { this.direccionCliente = d; }
    public Integer getEtaMinutos() { return etaMinutos; }
    public void setEtaMinutos(Integer e) { this.etaMinutos = e; }
    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double d) { this.distanciaKm = d; }
    public String getNombreRepartidor() { return nombreRepartidor; }
    public void setNombreRepartidor(String n) { this.nombreRepartidor = n; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getUrlNavegacion() { return urlNavegacion; }
    public void setUrlNavegacion(String urlNavegacion) { this.urlNavegacion = urlNavegacion; }
}