package com.sistemaapolloAngular.sistema_apolloAngular.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
                // CONFIGURACIÓN CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CONFIGURACIÓN DE CABECERAS DE SEGURIDAD
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://www.google.com https://www.gstatic.com; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                        "img-src 'self' data: blob: https:; " +
                                        "font-src 'self' https:; " +
                                        "connect-src 'self' https:; " + // Permite TODAS las conexiones HTTPS
                                        "frame-src 'self' https:; " + // Permite TODOS los iframes HTTPS
                                        "object-src 'none';")
                        )
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        .contentTypeOptions(contentType -> {})
                )


                // CSRF CORREGIDO para Spring Boot 3.x
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .ignoringRequestMatchers(
                                "/api/auth/**",
                                "/api/direcciones/**",
                                "/cajero/marcar-pagado/**",
                                "/cajero/marcar-cancelado/**",
                                "/cocinero/iniciar-preparacion/**",
                                "/cocinero/marcar-listo/**",
                                "/delivery/iniciar-entrega/**",
                                "/delivery/marcar-entregado/**"
                        )
                )

                // CONFIGURACIÓN DE SESIÓN
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                )
                .rememberMe(remember -> remember.disable())
                // RUTAS Y PERMISOS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index",
                                "/locales", "/nuestros-locales",
                                "/login", "/registrate", "/registro",
                                "/api/auth/**",
                                "/api/direcciones/**",
                                "/api/combos",
                                "/css/**", "/script/**", "/imagenes/**", "/archivos/**", "/webfonts/**",
                                "/error", "/libro-reclamaciones", "/terminos",
                                "/politica-datos", "/politica-cookies",
                                "/menu", "/menu/**"
                        ).permitAll()
                        .requestMatchers("/admin-menu", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cajero", "/cajero/**").hasRole("CAJERO")
                        .requestMatchers("/cocinero", "/cocinero/**").hasRole("COCINERO")
                        .requestMatchers("/delivery", "/delivery/**").hasRole("DELIVERY")
                        .requestMatchers("/pedidos-comunes", "/api/pedidos-comunes/**")
                        .hasAnyRole("ADMIN", "CAJERO", "COCINERO", "DELIVERY")
                        .requestMatchers(
                                "/carrito", "/carrito/**",
                                "/pago", "/pago/**",
                                "/pedido/**", "/pedidos/**",
                                "/api/pedidos/**", "/api/pedido/**",
                                "/crear-pedido", "/confirmar-pedido", "/confirmacion-pedido",
                                "/mis-cuentas", "/mis-datos", "/mis-direcciones",
                                "/mis-favoritos", "/mis-pedidos"
                        ).authenticated()
                        .anyRequest().authenticated()
                )

                // LOGIN FORM
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/postLogin", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // LOGOUT
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "APOLLO_SESSION")
                        .permitAll()
                )

                // MANEJO DE EXCEPCIONES
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/acceso-denegado")
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",
                "http://localhost:4200",
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

