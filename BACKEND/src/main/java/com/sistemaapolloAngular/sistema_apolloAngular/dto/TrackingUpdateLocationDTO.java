package com.sistemaapolloAngular.sistema_apolloAngular.dto;

public class TrackingUpdateLocationDTO {

    private String numeroPedido;
    private Double latRepartidor;
    private Double lngRepartidor;

    // Getters y Setters
    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public Double getLatRepartidor() { return latRepartidor; }
    public void setLatRepartidor(Double latRepartidor) { this.latRepartidor = latRepartidor; }

    public Double getLngRepartidor() { return lngRepartidor; }
    public void setLngRepartidor(Double lngRepartidor) { this.lngRepartidor = lngRepartidor; }
}
