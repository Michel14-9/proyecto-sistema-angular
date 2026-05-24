package com.sistemaapolloAngular.sistema_apolloAngular.dto;

import lombok.Data;

@Data
public class PagoRequest {

    private String titulo;
    private Integer cantidad;
    private Double precio;
}