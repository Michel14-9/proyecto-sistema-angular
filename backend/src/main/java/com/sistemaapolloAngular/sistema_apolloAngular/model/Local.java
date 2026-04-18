package com.sistemaapolloAngular.sistema_apolloAngular.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "locales")
public class Local {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    @Column(name = "distrito")
    private String distrito;

    @Column(name = "referencia")
    private String referencia;

    // Tipos de servicio disponibles
    @Column(name = "comer_en_tienda")
    private Boolean comerEnTienda = true;

    @Column(name = "retiro_en_tienda")
    private Boolean retiroEnTienda = true;

    @Column(name = "delivery")
    private Boolean delivery = true;

    // Horarios
    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(name = "dias_atencion")
    private String diasAtencion; // Ej: "Lunes a Domingo"

    // Contacto
    @Column(nullable = false)
    private String telefono;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "correo")
    private String correo;

    // Coordenadas para mapa
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // Estado del local
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "orden_visual")
    private Integer ordenVisual = 1;

    // Imágenes
    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(name = "logo_url")
    private String logoUrl;


    public String getHorarioCompleto() {
        return diasAtencion + " de " + horaApertura + " a " + horaCierre;
    }

    public String getTiposServicio() {
        StringBuilder servicios = new StringBuilder();
        if (comerEnTienda != null && comerEnTienda) servicios.append("Comer en tienda, ");
        if (retiroEnTienda != null && retiroEnTienda) servicios.append("Retiro en tienda, ");
        if (delivery != null && delivery) servicios.append("Delivery, ");

        if (servicios.length() > 0) {
            servicios.setLength(servicios.length() - 2); // Remover última coma
        }
        return servicios.toString();
    }

    // --- Constructores ---
    public Local() {}

    public Local(String nombre, String direccion, String telefono) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Boolean getComerEnTienda() { return comerEnTienda; }
    public void setComerEnTienda(Boolean comerEnTienda) { this.comerEnTienda = comerEnTienda; }

    public Boolean getRetiroEnTienda() { return retiroEnTienda; }
    public void setRetiroEnTienda(Boolean retiroEnTienda) { this.retiroEnTienda = retiroEnTienda; }

    public Boolean getDelivery() { return delivery; }
    public void setDelivery(Boolean delivery) { this.delivery = delivery; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public String getDiasAtencion() { return diasAtencion; }
    public void setDiasAtencion(String diasAtencion) { this.diasAtencion = diasAtencion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Integer getOrdenVisual() { return ordenVisual; }
    public void setOrdenVisual(Integer ordenVisual) { this.ordenVisual = ordenVisual; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    // --- toString ---
    @Override
    public String toString() {
        return "Local{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", direccion='" + direccion + '\'' +
                ", distrito='" + distrito + '\'' +
                ", telefono='" + telefono + '\'' +
                ", activo=" + activo +
                '}';
    }
}
