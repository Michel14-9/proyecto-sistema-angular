package com.sistemaapolloAngular.sistema_apolloAngular.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 404 - RECURSO NO ENCONTRADO =====
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, WebRequest request) {
        log.warn("🔴 Recurso no encontrado: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ===== 400 - ERROR DE NEGOCIO =====
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("⚠️ Error de negocio: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "BUSINESS_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ===== 400 - ERROR DE VALIDACIÓN =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("📋 Error de validación: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Error de validación en los campos",
                HttpStatus.BAD_REQUEST.toString(),
                request.getDescription(false)
        );
        error.setFieldErrors(fieldErrors);

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ===== 400 - VIOLACIÓN DE CONSTRAINT =====
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("🔒 Violación de constraint: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "CONSTRAINT_VIOLATION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ===== 401 - NO AUTENTICADO =====
    @ExceptionHandler({BadCredentialsException.class,
            org.springframework.security.core.AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, WebRequest request) {
        log.warn("🔐 Error de autenticación: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "UNAUTHORIZED",
                "Credenciales inválidas o usuario no autenticado",
                HttpStatus.UNAUTHORIZED.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // ===== 403 - ACCESO DENEGADO =====
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("🚫 Acceso denegado: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "ACCESS_DENIED",
                "No tienes permisos para acceder a este recurso",
                HttpStatus.FORBIDDEN.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // ===== 404 - ENDPOINT NO ENCONTRADO =====
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, WebRequest request) {
        log.warn("🌐 Endpoint no encontrado: {}", ex.getRequestURL());

        ErrorResponse error = new ErrorResponse(
                "ENDPOINT_NOT_FOUND",
                "El endpoint solicitado no existe: " + ex.getRequestURL(),
                HttpStatus.NOT_FOUND.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ===== 404 - RECURSO ESTÁTICO NO ENCONTRADO (favicon.ico) =====
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, WebRequest request) {

        // Si es favicon.ico, es normal que no exista
        if (ex.getMessage().contains("favicon.ico")) {
            log.debug("🔍 Favicon solicitado pero no existe");

            ErrorResponse error = new ErrorResponse(
                    "RESOURCE_NOT_FOUND",
                    "Recurso no encontrado",
                    HttpStatus.NOT_FOUND.toString(),
                    request.getDescription(false)
            );
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        // Para otros recursos estáticos
        log.warn("📁 Recurso estático no encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "STATIC_RESOURCE_NOT_FOUND",
                "El recurso solicitado no existe",
                HttpStatus.NOT_FOUND.toString(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ===== 405 - MÉTODO HTTP NO SOPORTADO =====
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        log.warn("🚫 Método HTTP no soportado: {}", ex.getMessage());

        String supportedMethods = ex.getSupportedMethods() != null
                ? String.join(", ", ex.getSupportedMethods())
                : "No especificados";

        ErrorResponse error = new ErrorResponse(
                "METHOD_NOT_ALLOWED",
                "El método HTTP " + ex.getMethod() + " no es soportado. " +
                        "Métodos permitidos: " + supportedMethods,
                HttpStatus.METHOD_NOT_ALLOWED.toString(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // ===== 500 - ERROR INTERNO =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // Log completo del error con stack trace
        log.error("💥 Error interno del servidor: ", ex);

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "Ha ocurrido un error interno en el servidor",
                HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                request.getDescription(false)
        );

        // SOLO EN DESARROLLO - Descomentar para ver el error real
        // error.setMessage(ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}