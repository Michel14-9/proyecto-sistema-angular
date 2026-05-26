package com.sistemaapolloAngular.sistema_apolloAngular.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // -------------------------------------------------------
                // CORS
                // -------------------------------------------------------
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // -------------------------------------------------------
                // CABECERAS DE SEGURIDAD
                // -------------------------------------------------------
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://www.google.com https://www.gstatic.com; " +
                                                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                                "img-src 'self' data: blob: https:; " +
                                                "font-src 'self' https:; " +
                                                "connect-src 'self' https:; " +
                                                "frame-src 'self' https:; " +
                                                "object-src 'none';"
                                )
                        )
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        .contentTypeOptions(contentType -> {})
                )

                // -------------------------------------------------------
                // CSRF — usa cookie para que Angular pueda leerla
                // Angular HttpClient la envía automáticamente si configuras
                // withCredentials: true y el interceptor de XSRF
                // -------------------------------------------------------
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/auth/**",
                                "/api/direcciones/**",
                                "/api/pagos/**",
                                "/cajero/marcar-pagado/**",
                                "/cajero/marcar-cancelado/**",
                                "/cocinero/iniciar-preparacion/**",
                                "/cocinero/marcar-listo/**",
                                "/delivery/iniciar-entrega/**",
                                "/delivery/marcar-entregado/**"
                        )
                )

                // -------------------------------------------------------
                // SESIÓN — basada en cookies (stateful), apta para Angular
                // -------------------------------------------------------
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .rememberMe(remember -> remember.disable())

                // -------------------------------------------------------
                // RUTAS Y PERMISOS
                // -------------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/direcciones/**",
                                "/api/combos",
                                "/api/pagos/**",
                                "/api/menu/**",
                                "/login",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/cajero/**").hasRole("CAJERO")
                        .requestMatchers("/api/cocinero/**").hasRole("COCINERO")
                        .requestMatchers("/api/delivery/**").hasRole("DELIVERY")
                        .requestMatchers("/api/pedidos-comunes/**")
                        .hasAnyRole("ADMIN", "CAJERO", "COCINERO", "DELIVERY")
                        .requestMatchers(
                                "/api/carrito/**",
                                "/api/pedidos/**",
                                "/api/pedido/**",
                                "/api/mis-pedidos/**",
                                "/api/mis-datos/**",
                                "/api/mis-direcciones/**",
                                "/api/mis-favoritos/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )

                // -------------------------------------------------------
                // LOGIN — procesa POST /api/auth/login
                // Devuelve JSON en lugar de redirigir a una vista HTML
                // -------------------------------------------------------
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")   // Angular hace POST aquí
                        .usernameParameter("username")
                        .passwordParameter("password")

                        // LOGIN EXITOSO → JSON con datos del usuario y su rol
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            String rol = authentication.getAuthorities()
                                    .stream()
                                    .findFirst()
                                    .map(a -> a.getAuthority())
                                    .orElse("ROLE_USER");

                            Map<String, Object> body = new HashMap<>();
                            body.put("status", "ok");
                            body.put("usuario", authentication.getName());
                            body.put("rol", rol);
                            body.put("mensaje", "Login exitoso");

                            new ObjectMapper().writeValue(response.getWriter(), body);
                        })

                        // LOGIN FALLIDO → JSON con mensaje de error
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = new HashMap<>();
                            body.put("status", "error");
                            body.put("mensaje", "Usuario o contraseña incorrectos");

                            new ObjectMapper().writeValue(response.getWriter(), body);
                        })
                        .permitAll()
                )

                // -------------------------------------------------------
                // LOGOUT — POST /api/auth/logout
                // Devuelve JSON en lugar de redirigir
                // -------------------------------------------------------
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout", "POST"))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")

                        // LOGOUT EXITOSO → JSON
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = new HashMap<>();
                            body.put("status", "ok");
                            body.put("mensaje", "Sesión cerrada correctamente");

                            new ObjectMapper().writeValue(response.getWriter(), body);
                        })
                        .permitAll()
                )

                // -------------------------------------------------------
                // EXCEPCIONES — responde JSON en vez de redirigir al login
                // -------------------------------------------------------
                .exceptionHandling(exception -> exception
                        // No autenticado → 401 JSON
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = new HashMap<>();
                            body.put("status", "error");
                            body.put("mensaje", "No autenticado. Inicia sesión para continuar.");

                            new ObjectMapper().writeValue(response.getWriter(), body);
                        })
                        // Sin permisos → 403 JSON
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = new HashMap<>();
                            body.put("status", "error");
                            body.put("mensaje", "Acceso denegado. No tienes permisos para este recurso.");

                            new ObjectMapper().writeValue(response.getWriter(), body);
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",   // Angular dev
                "http://localhost:8080",   // Spring Boot (mismo servidor)
                "https://tudominio.com"    // Producción
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "authorization",
                "content-type",
                "x-auth-token",
                "x-requested-with",
                "x-xsrf-token"
        ));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token", "xsrf-token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}