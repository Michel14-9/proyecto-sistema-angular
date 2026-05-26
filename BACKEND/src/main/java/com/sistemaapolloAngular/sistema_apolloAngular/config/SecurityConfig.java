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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

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

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/auth/**",
                                "/api/direcciones/**",
                                "/api/pagos/**",
                                "/admin-menu/**",              // ← admin puede hacer POST/DELETE
                                "/cajero/**",
                                "/cocinero/**",
                                "/delivery/**"
                        )
                )

                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .rememberMe(remember -> remember.disable())

                .authorizeHttpRequests(auth -> auth
                        // ── Rutas públicas ──────────────────────────────
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/direcciones/**",
                                "/api/combos",
                                "/api/pagos/**",
                                "/api/menu/**",
                                "/login",
                                "/error"
                        ).permitAll()

                        // ── ADMIN → solo accede a /admin-menu/** ────────
                        .requestMatchers("/admin-menu/**").hasRole("ADMIN")

                        // ── CAJERO → solo accede a /cajero/** ───────────
                        .requestMatchers("/cajero/**").hasRole("CAJERO")

                        // ── COCINERO → solo accede a /cocinero/** ───────
                        .requestMatchers("/cocinero/**").hasRole("COCINERO")

                        // ── DELIVERY → solo accede a /delivery/** ───────
                        .requestMatchers("/delivery/**").hasRole("DELIVERY")

                        // ── Pedidos comunes → todos los roles internos ──
                        .requestMatchers("/pedidos-comunes/**", "/api/pedidos-comunes/**")
                        .hasAnyRole("ADMIN", "CAJERO", "COCINERO", "DELIVERY")

                        // ── Rutas de cliente autenticado ────────────────
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

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")

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

                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout", "POST"))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")

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

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = new HashMap<>();
                            body.put("status", "error");
                            body.put("mensaje", "No autenticado. Inicia sesión para continuar.");

                            new ObjectMapper().writeValue(response.getWriter(), body);
                        })
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
                "http://localhost:4200",
                "http://localhost:8080",
                "https://tudominio.com"
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