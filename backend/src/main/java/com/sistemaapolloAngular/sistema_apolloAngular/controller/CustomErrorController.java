package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        Map<String, Object> errorResponse = new HashMap<>();

        try {
            if (status != null) {
                int statusCode = Integer.parseInt(status.toString());

                // Determinar mensaje según el código de error
                String message = "Error interno del servidor";
                String error = "INTERNAL_SERVER_ERROR";

                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    message = "Recurso no encontrado";
                    error = "NOT_FOUND";
                } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    message = "Acceso denegado. No tienes permisos para acceder a este recurso.";
                    error = "FORBIDDEN";
                } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    message = "Error interno del servidor. Por favor, intenta más tarde.";
                    error = "INTERNAL_SERVER_ERROR";
                } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                    message = "No autenticado. Inicia sesión para continuar.";
                    error = "UNAUTHORIZED";
                } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                    message = "Solicitud inválida. Verifica los datos enviados.";
                    error = "BAD_REQUEST";
                } else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                    message = "Método HTTP no permitido.";
                    error = "METHOD_NOT_ALLOWED";
                }

                // Agregar mensaje de error detallado si existe
                if (errorMessage != null && !errorMessage.toString().isEmpty()) {
                    message = errorMessage.toString();
                }

                errorResponse.put("status", "error");
                errorResponse.put("codigo", statusCode);
                errorResponse.put("error", error);
                errorResponse.put("mensaje", message);
                errorResponse.put("timestamp", System.currentTimeMillis());
                errorResponse.put("ruta", request.getRequestURI());

                return ResponseEntity.status(statusCode).body(errorResponse);
            }
        } catch (NumberFormatException e) {
            // Si hay error al parsear el código
            errorResponse.put("status", "error");
            errorResponse.put("codigo", 500);
            errorResponse.put("error", "INTERNAL_SERVER_ERROR");
            errorResponse.put("mensaje", "Error interno del servidor");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

        // Respuesta genérica para errores no identificados
        errorResponse.put("status", "error");
        errorResponse.put("codigo", 500);
        errorResponse.put("error", "INTERNAL_SERVER_ERROR");
        errorResponse.put("mensaje", "Error interno del servidor");
        errorResponse.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}