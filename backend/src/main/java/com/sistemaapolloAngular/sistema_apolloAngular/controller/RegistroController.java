package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private UsuarioRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    @GetMapping("/datos-usuario")
    public ResponseEntity<Map<String, Object>> obtenerDatosUsuario(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = authentication.getName();
            Usuario usuario = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            response.put("success", true);
            response.put("data", Map.of(
                    "nombre", usuario.getNombres(),
                    "apellidos", usuario.getApellidos(),
                    "email", usuario.getUsername(),
                    "tipoDocumento", usuario.getTipoDocumento(),
                    "numeroDocumento", usuario.getNumeroDocumento(),
                    "telefono", usuario.getTelefono(),
                    "fechaNacimiento", usuario.getFechaNacimiento().toString()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener datos del usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PutMapping("/actualizar-datos")
    public ResponseEntity<Map<String, Object>> actualizarDatos(@RequestBody ActualizarDatosRequest request,
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = authentication.getName();
            Usuario usuario = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Cambio de contraseña
            if (request.getNuevaPassword() != null && !request.getNuevaPassword().isEmpty()) {
                if (request.getPasswordActual() == null || request.getPasswordActual().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Debes ingresar tu contraseña actual para cambiarla");
                    return ResponseEntity.badRequest().body(response);
                }

                if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                    response.put("success", false);
                    response.put("message", "La contraseña actual es incorrecta");
                    return ResponseEntity.badRequest().body(response);
                }

                usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
            }

            // Actualizar datos personales
            usuario.setNombres(request.getNombre());
            usuario.setApellidos(request.getApellidos());
            usuario.setTipoDocumento(request.getTipoDocumento());
            usuario.setNumeroDocumento(request.getNumeroDocumento());
            usuario.setTelefono(request.getTelefono());
            usuario.setFechaNacimiento(request.getFechaNacimiento());

            userRepository.save(usuario);

            response.put("success", true);
            response.put("message", "Datos actualizados correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar datos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrar(
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento,
            @RequestParam String telefono,
            @RequestParam String fechaNacimiento,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(value = "rol", defaultValue = "CLIENTE") String rol,
            @RequestParam(value = "g-recaptcha-response", required = false) String captchaResponse,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println(" === INICIO REGISTRO ===");
            System.out.println(" Email: " + email);

            // Validar reCAPTCHA
            if (captchaResponse == null || captchaResponse.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Error de verificación de seguridad. El token reCAPTCHA es requerido.");
                return ResponseEntity.badRequest().body(response);
            }

            String remoteIp = request.getRemoteAddr();
            boolean captchaValido = captchaService.validateCaptchaV3(captchaResponse, remoteIp);

            if (!captchaValido) {
                response.put("success", false);
                response.put("message", "Verificación de seguridad fallida. Intente de nuevo.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar usuario existente
            if (userRepository.findByUsername(email).isPresent()) {
                response.put("success", false);
                response.put("message", "El correo ya está registrado");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar documento
            if ("DNI".equalsIgnoreCase(tipoDocumento) && numeroDocumento.length() != 8) {
                response.put("success", false);
                response.put("message", "El DNI debe tener 8 dígitos");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar fecha
            LocalDate fechaNac;
            try {
                fechaNac = LocalDate.parse(fechaNacimiento);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Formato de fecha inválido. Use yyyy-MM-dd");
                return ResponseEntity.badRequest().body(response);
            }

            // Crear usuario
            Usuario usuario = new Usuario();
            usuario.setNombres(nombres);
            usuario.setApellidos(apellidos);
            usuario.setTipoDocumento(tipoDocumento);
            usuario.setNumeroDocumento(numeroDocumento);
            usuario.setTelefono(telefono);
            usuario.setFechaNacimiento(fechaNac);
            usuario.setUsername(email);
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setRol(rol.toUpperCase());

            userRepository.save(usuario);

            response.put("success", true);
            response.put("message", "Registro exitoso de: " + nombres + " " + apellidos);
            response.put("email", email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" ERROR en registro: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Error en registro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String username,
                                                     @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println(" === INICIO LOGIN ===");
            System.out.println(" Username: " + username);

            Optional<Usuario> optionalUsuario = userRepository.findByUsername(username);

            if (optionalUsuario.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario o contraseña incorrecta");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Usuario usuario = optionalUsuario.get();

            if (passwordEncoder.matches(password, usuario.getPassword())) {
                response.put("success", true);
                response.put("message", "Login exitoso");
                response.put("username", usuario.getUsername());
                response.put("nombre", usuario.getNombres() + " " + usuario.getApellidos());
                response.put("rol", usuario.getRol());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Usuario o contraseña incorrecta");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en login: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/verificar-email")
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean existe = userRepository.findByUsername(email).isPresent();

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

    // ==================== DTOs ====================

    public static class ActualizarDatosRequest {
        private String nombre;
        private String apellidos;
        private String tipoDocumento;
        private String numeroDocumento;
        private String telefono;
        private LocalDate fechaNacimiento;
        private String passwordActual;
        private String nuevaPassword;

        // Getters y Setters
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getApellidos() { return apellidos; }
        public void setApellidos(String apellidos) { this.apellidos = apellidos; }

        public String getTipoDocumento() { return tipoDocumento; }
        public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

        public String getNumeroDocumento() { return numeroDocumento; }
        public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }

        public LocalDate getFechaNacimiento() { return fechaNacimiento; }
        public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

        public String getPasswordActual() { return passwordActual; }
        public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }

        public String getNuevaPassword() { return nuevaPassword; }
        public void setNuevaPassword(String nuevaPassword) { this.nuevaPassword = nuevaPassword; }
    }
}