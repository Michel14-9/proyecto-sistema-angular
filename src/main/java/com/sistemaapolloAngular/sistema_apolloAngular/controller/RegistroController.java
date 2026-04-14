package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private UsuarioRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    // ENDPOINT: OBTENER DATOS DEL USUARIO AUTENTICADO

    /**
     * Obtiene los datos del usuario actualmente autenticado.
     * Este endpoint es usado por la página "Mis Datos" para mostrar la información del usuario.
     *
     * @param authentication Objeto de autenticación de Spring Security que contiene los datos del usuario logueado
     * @return ResponseEntity con los datos del usuario o mensaje de error
     */
    @GetMapping("/datos-usuario")
    public ResponseEntity<?> obtenerDatosUsuario(Authentication authentication) {
        try {
            System.out.println(" === OBTENIENDO DATOS USUARIO ===");
            System.out.println(" Usuario autenticado: " + authentication.getName());

            // Obtener el usuario desde la base de datos usando el email (username) del usuario autenticado
            String username = authentication.getName();
            Usuario usuario = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Crear respuesta con los datos del usuario
            UsuarioDatosResponse response = new UsuarioDatosResponse();
            response.setNombre(usuario.getNombres());
            response.setApellidos(usuario.getApellidos());
            response.setEmail(usuario.getUsername());
            response.setTipoDocumento(usuario.getTipoDocumento());
            response.setNumeroDocumento(usuario.getNumeroDocumento());
            response.setTelefono(usuario.getTelefono());
            response.setFechaNacimiento(usuario.getFechaNacimiento());

            System.out.println(" Datos enviados para: " + usuario.getNombres() + " " + usuario.getApellidos());
            return ok(response);

        } catch (Exception e) {
            System.out.println(" Error obteniendo datos del usuario: " + e.getMessage());
            return badRequest().body("Error al obtener datos del usuario: " + e.getMessage());
        }
    }


    // ENDPOINT: ACTUALIZAR DATOS DEL USUARIO

    /**
     * Actualiza los datos personales del usuario autenticado.
     * Permite actualizar información básica y/o cambiar la contraseña.
     *
     * @param request Objeto con los nuevos datos del usuario
     * @param authentication Objeto de autenticación de Spring Security
     * @return ResponseEntity con mensaje de éxito o error
     */
    @PutMapping("/actualizar-datos")
    public ResponseEntity<?> actualizarDatos(@RequestBody ActualizarDatosRequest request,
                                             Authentication authentication) {
        try {
            System.out.println(" === ACTUALIZANDO DATOS USUARIO ===");
            System.out.println(" Usuario: " + authentication.getName());
            System.out.println(" Datos recibidos: " + request);

            // Obtener el usuario actual desde la base de datos
            String username = authentication.getName();
            Usuario usuario = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


            // VALIDACIÓN Y ACTUALIZACIÓN DE CONTRASEÑA

            if (request.getNuevaPassword() != null && !request.getNuevaPassword().isEmpty()) {
                System.out.println(" Solicitado cambio de contraseña");

                // Verificar que se proporcionó la contraseña actual
                if (request.getPasswordActual() == null || request.getPasswordActual().isEmpty()) {
                    return badRequest().body("Debes ingresar tu contraseña actual para cambiarla");
                }

                // Verificar que la contraseña actual sea correcta
                if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                    return badRequest().body("La contraseña actual es incorrecta");
                }

                // Actualizar contraseña
                usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
                System.out.println(" Contraseña actualizada correctamente");
            }


            // ACTUALIZACIÓN DE DATOS PERSONALES

            usuario.setNombres(request.getNombre());
            usuario.setApellidos(request.getApellidos());
            usuario.setTipoDocumento(request.getTipoDocumento());
            usuario.setNumeroDocumento(request.getNumeroDocumento());
            usuario.setTelefono(request.getTelefono());
            usuario.setFechaNacimiento(request.getFechaNacimiento());

            // Guardar cambios en la base de datos
            userRepository.save(usuario);

            System.out.println(" Datos actualizados correctamente para: " + usuario.getNombres());
            return ok("Datos actualizados correctamente");

        } catch (Exception e) {
            System.out.println(" Error actualizando datos: " + e.getMessage());
            return badRequest().body("Error al actualizar los datos: " + e.getMessage());
        }
    }


    // ENDPOINT: REGISTRO DE NUEVO USUARIO

    /**
     * Registra un nuevo usuario en el sistema.
     * Incluye validación de reCAPTCHA v3 para prevenir spam.
     *
     * @param nombres Nombre(s) del usuario
     * @param apellidos Apellido(s) del usuario
     * @param tipoDocumento Tipo de documento (DNI, Pasaporte, Carné de extranjería)
     * @param numeroDocumento Número del documento de identidad
     * @param telefono Número de teléfono (9 dígitos)
     * @param fechaNacimiento Fecha de nacimiento en formato String (yyyy-MM-dd)
     * @param email Correo electrónico (usado como nombre de usuario)
     * @param password Contraseña del usuario
     * @param rol Rol del usuario (por defecto "CLIENTE")
     * @param captchaResponse Token de respuesta de Google reCAPTCHA v3
     * @param request Objeto HttpServletRequest para obtener la IP remota
     * @return ResponseEntity con mensaje de éxito o error
     */
    @PostMapping("/registro")
    public ResponseEntity<String> registrar(
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

        System.out.println(" === INICIO REGISTRO ===");
        System.out.println(" Email: " + email);
        System.out.println(" reCAPTCHA token recibido: " +
                (captchaResponse != null ? captchaResponse.substring(0, Math.min(20, captchaResponse.length())) + "..." : "NULL"));

        // 1. Validación de reCAPTCHA
        if (captchaResponse == null || captchaResponse.trim().isEmpty()) {
            System.out.println(" ERROR: reCAPTCHA response está vacío");
            return badRequest().body("Error de verificación de seguridad. El token reCAPTCHA es requerido.");
        }

        String remoteIp = request.getRemoteAddr();
        System.out.println(" IP remota: " + remoteIp);
        boolean captchaValido = captchaService.validateCaptchaV3(captchaResponse, remoteIp);

        if (!captchaValido) {
            System.out.println("reCAPTCHA inválido");
            return badRequest().body(" Verificación de seguridad fallida. Intente de nuevo.");
        }

        // 2. Validación de usuario existente (por email/username)
        if (userRepository.findByUsername(email).isPresent()) {
            return badRequest().body(" El correo ya está registrado");
        }

        // 3. Validación de longitud de documento
        if ("DNI".equalsIgnoreCase(tipoDocumento) && numeroDocumento.length() != 8) {
            return badRequest().body("El DNI debe tener 8 dígitos");
        }
        if ("Pasaporte".equalsIgnoreCase(tipoDocumento) && numeroDocumento.length() < 6) {
            return badRequest().body("El Pasaporte debe tener al menos 6 caracteres");
        }

        // 4. Conversión y validación de fecha de nacimiento
        LocalDate fechaNac;
        try {
            fechaNac = LocalDate.parse(fechaNacimiento);
        } catch (Exception e) {
            return badRequest().body(" Formato de fecha inválido. Use yyyy-MM-dd");
        }

        // 5. Creación y guardado del usuario
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
        System.out.println(" === REGISTRO EXITOSO ===");
        return ok(" Registro exitoso de: " + nombres + " " + apellidos);
    }




    /**
     * Autentica a un usuario verificando sus credenciales.
     * Nota: En un sistema real, aquí se generaría y devolvería un JWT.
     *
     * @param username Correo electrónico del usuario
     * @param password Contraseña ingresada
     * @return ResponseEntity con mensaje de bienvenida o error de credenciales
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {

        System.out.println(" === INICIO LOGIN ===");
        System.out.println(" Username: " + username);

        Optional<Usuario> optionalUsuario = userRepository.findByUsername(username);

        if (optionalUsuario.isEmpty()) {
            System.out.println(" Usuario no encontrado: " + username);
            return badRequest().body("⚠️ Usuario o contraseña incorrecta");
        }

        Usuario usuario = optionalUsuario.get();
        if (passwordEncoder.matches(password, usuario.getPassword())) {
            System.out.println(" Login exitoso para: " + username);
            return ok(" Bienvenido " + usuario.getNombres() + " (Rol: " + usuario.getRol() + ")");
        } else {
            System.out.println(" Contraseña incorrecta para: " + username);
            return badRequest().body("⚠️ Usuario o contraseña incorrecta");
        }
    }





    /**
     * DTO para enviar datos del usuario al frontend
     */
    public static class UsuarioDatosResponse {
        private String nombre;
        private String apellidos;
        private String email;
        private String tipoDocumento;
        private String numeroDocumento;
        private String telefono;
        private LocalDate fechaNacimiento;

        // Getters y Setters
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getApellidos() { return apellidos; }
        public void setApellidos(String apellidos) { this.apellidos = apellidos; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getTipoDocumento() { return tipoDocumento; }
        public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

        public String getNumeroDocumento() { return numeroDocumento; }
        public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }

        public LocalDate getFechaNacimiento() { return fechaNacimiento; }
        public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    }

    /**
     * DTO para recibir datos de actualización del frontend
     */
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

        @Override
        public String toString() {
            return "ActualizarDatosRequest{" +
                    "nombre='" + nombre + '\'' +
                    ", apellidos='" + apellidos + '\'' +
                    ", tipoDocumento='" + tipoDocumento + '\'' +
                    ", numeroDocumento='" + numeroDocumento + '\'' +
                    ", telefono='" + telefono + '\'' +
                    ", fechaNacimiento=" + fechaNacimiento +
                    ", cambiarPassword=" + (nuevaPassword != null && !nuevaPassword.isEmpty()) +
                    '}';
        }
    }
}
