package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.UsuarioRegistroDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario
     */
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrarUsuario(@RequestBody UsuarioRegistroDTO dto) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== REGISTRO DE USUARIO API ===");
            System.out.println("Email: " + dto.getUsername());
            System.out.println("Nombres: " + dto.getNombres());

            // Validar campos obligatorios
            if (dto.getNombres() == null || dto.getNombres().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Los nombres son obligatorios");
                return ResponseEntity.badRequest().body(response);
            }

            if (dto.getApellidos() == null || dto.getApellidos().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Los apellidos son obligatorios");
                return ResponseEntity.badRequest().body(response);
            }

            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El correo electrónico es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            if (dto.getPassword() == null || dto.getPassword().trim().length() < 6) {
                response.put("success", false);
                response.put("message", "La contraseña debe tener al menos 6 caracteres");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar si el correo ya está registrado
            if (usuarioRepository.findByUsername(dto.getUsername()).isPresent()) {
                response.put("success", false);
                response.put("message", "El correo electrónico ya está registrado");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Validar tipo de documento y número
            if (dto.getTipoDocumento() != null && dto.getNumeroDocumento() != null) {
                if ("DNI".equalsIgnoreCase(dto.getTipoDocumento()) && dto.getNumeroDocumento().length() != 8) {
                    response.put("success", false);
                    response.put("message", "El DNI debe tener 8 dígitos");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Crear usuario
            Usuario usuario = new Usuario();
            usuario.setNombres(dto.getNombres().trim());
            usuario.setApellidos(dto.getApellidos().trim());
            usuario.setTipoDocumento(dto.getTipoDocumento());
            usuario.setNumeroDocumento(dto.getNumeroDocumento());
            usuario.setTelefono(dto.getTelefono());
            usuario.setFechaNacimiento(dto.getFechaNacimiento());
            usuario.setUsername(dto.getUsername().trim());
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
            usuario.setRol("CLIENTE"); // Rol por defecto

            Usuario usuarioGuardado = usuarioRepository.save(usuario);

            System.out.println(" Usuario registrado exitosamente: " + usuarioGuardado.getUsername());

            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("usuario", Map.of(
                    "id", usuarioGuardado.getId(),
                    "nombres", usuarioGuardado.getNombres(),
                    "apellidos", usuarioGuardado.getApellidos(),
                    "email", usuarioGuardado.getUsername(),
                    "rol", usuarioGuardado.getRol()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println(" Error en registro: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al registrar usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtiene el perfil del usuario autenticado
     */
    @GetMapping("/perfil")
    public ResponseEntity<Map<String, Object>> obtenerPerfil(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Usuario usuario = usuarioOpt.get();

            response.put("success", true);
            response.put("usuario", Map.of(
                    "id", usuario.getId(),
                    "nombres", usuario.getNombres(),
                    "apellidos", usuario.getApellidos(),
                    "email", usuario.getUsername(),
                    "telefono", usuario.getTelefono(),
                    "tipoDocumento", usuario.getTipoDocumento(),
                    "numeroDocumento", usuario.getNumeroDocumento(),
                    "fechaNacimiento", usuario.getFechaNacimiento() != null ? usuario.getFechaNacimiento().toString() : null,
                    "rol", usuario.getRol()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener perfil: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verifica si un correo ya está registrado
     */
    @GetMapping("/verificar-email")
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean existe = usuarioRepository.findByUsername(email).isPresent();

            response.put("success", true);
            response.put("disponible", !existe);
            response.put("email", email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al verificar email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}