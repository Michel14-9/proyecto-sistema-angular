package com.sistemaapolloAngular.sistema_apolloAngular.dto;

public class TrackingCreateDTO {

    private Long pedidoId;
    private String numeroPedido;
    private String direccionCliente;   // texto plano, el service geocodifica
    private String nombreRepartidor;

    // Si ya se tienen coordenadas, se envían directo (evita llamada Geocoding)
    private Double latCliente;
    private Double lngCliente;

    // Getters y Setters
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public String getDireccionCliente() { return direccionCliente; }
    public void setDireccionCliente(String direccionCliente) { this.direccionCliente = direccionCliente; }

    public String getNombreRepartidor() { return nombreRepartidor; }
    public void setNombreRepartidor(String nombreRepartidor) { this.nombreRepartidor = nombreRepartidor; }

    public Double getLatCliente() { return latCliente; }
    public void setLatCliente(Double latCliente) { this.latCliente = latCliente; }

    public Double getLngCliente() { return lngCliente; }
    public void setLngCliente(Double lngCliente) { this.lngCliente = lngCliente; }
}