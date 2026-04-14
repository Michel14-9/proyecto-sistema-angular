package com.sistemaapolloAngular.sistema_apolloAngular.dto;

public class DireccionDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String direccion;
    private String referencia;
    private String ciudad;
    private String telefono;
    private boolean predeterminada;
    private boolean facturacion;

    // Constructor vacío
    public DireccionDTO() {}

    // Constructor desde Entidad
    public DireccionDTO(com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion direccion) {
        this.id = direccion.getId();
        this.nombre = direccion.getNombre();
        this.tipo = direccion.getTipo();
        this.direccion = direccion.getDireccion();
        this.referencia = direccion.getReferencia();
        this.ciudad = direccion.getCiudad();
        this.telefono = direccion.getTelefono();
        this.predeterminada = direccion.isPredeterminada();
        this.facturacion = direccion.isFacturacion();
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
}

