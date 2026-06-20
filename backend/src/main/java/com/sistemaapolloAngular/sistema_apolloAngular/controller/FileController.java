package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FileController {

    private static final String BOLETAS_DIR = "boletas/";
    private static final String IMAGENES_DIR = "imagenes/";
    private static final String REPORTES_DIR = "reportes/";


    @GetMapping("/boletas/{filename:.+}")
    public ResponseEntity<Resource> servirBoleta(@PathVariable String filename,
                                                 @RequestParam(defaultValue = "inline") String disposition) {
        try {
            // Validar seguridad: evitar path traversal (../)
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(BOLETAS_DIR + filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determinar tipo de contenido
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // Determinar disposición (inline o attachment)
                String contentDisposition;
                if ("attachment".equalsIgnoreCase(disposition)) {
                    contentDisposition = "attachment; filename=\"" + filename + "\"";
                } else {
                    contentDisposition = "inline; filename=\"" + filename + "\"";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/imagenes/{filename:.+}")
    public ResponseEntity<Resource> servirImagen(@PathVariable String filename) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(IMAGENES_DIR + filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "image/jpeg";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/reportes/{filename:.+}")
    public ResponseEntity<Resource> servirReporte(@PathVariable String filename,
                                                  @RequestParam(defaultValue = "attachment") String disposition) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(REPORTES_DIR + filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }

                String contentDisposition;
                if ("inline".equalsIgnoreCase(disposition)) {
                    contentDisposition = "inline; filename=\"" + filename + "\"";
                } else {
                    contentDisposition = "attachment; filename=\"" + filename + "\"";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/archivos/{filename:.+}")
    public ResponseEntity<Resource> servirArchivo(@PathVariable String filename) {
        try {
            // Validar seguridad
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            // Validar extensiones permitidas
            String[] allowedExtensions = {".pdf", ".jpg", ".jpeg", ".png", ".gif", ".xlsx", ".xls", ".docx", ".doc"};
            boolean isAllowed = false;
            for (String ext : allowedExtensions) {
                if (filename.toLowerCase().endsWith(ext)) {
                    isAllowed = true;
                    break;
                }
            }

            if (!isAllowed) {
                return ResponseEntity.badRequest().build();
            }

            // Buscar en diferentes directorios
            String[] directories = {BOLETAS_DIR, IMAGENES_DIR, REPORTES_DIR};
            for (String dir : directories) {
                Path filePath = Paths.get(dir + filename).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    String contentType = Files.probeContentType(filePath);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                            .body(resource);
                }
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}