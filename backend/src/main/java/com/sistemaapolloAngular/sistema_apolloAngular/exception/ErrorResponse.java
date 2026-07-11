package com.sistemaapolloAngular.sistema_apolloAngular.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private String httpStatus;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> fieldErrors;

    // Constructor para errores básicos
    public ErrorResponse(String code, String message, String httpStatus, String path) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    // Constructor sin path
    public ErrorResponse(String code, String message, String httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }
}