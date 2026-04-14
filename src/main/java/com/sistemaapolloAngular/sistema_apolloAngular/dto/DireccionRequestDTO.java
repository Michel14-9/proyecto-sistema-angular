package com.sistemaapolloAngular.sistema_apolloAngular.dto;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;

public class DireccionRequestDTO {
    private String nombre;
    private String tipo;
    private String direccion;
    private String referencia;
    private String ciudad;
    private String telefono;
    private boolean predeterminada;
    private boolean facturacion;


    public DireccionRequestDTO() {}


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


    public Direccion toEntity() {
        Direccion direccion = new Direccion();
        direccion.setNombre(this.nombre);
        direccion.setTipo(this.tipo);
        direccion.setDireccion(this.direccion);
        direccion.setReferencia(this.referencia);
        direccion.setCiudad(this.ciudad);
        direccion.setTelefono(this.telefono);
        direccion.setPredeterminada(this.predeterminada);
        direccion.setFacturacion(this.facturacion);
        return direccion;
    }
}

