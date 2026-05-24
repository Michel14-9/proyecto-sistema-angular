package com.sistemaapolloAngular.sistema_apolloAngular.dto;

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

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}