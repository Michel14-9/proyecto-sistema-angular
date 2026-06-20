package com.sistemaapolloAngular.sistema_apolloAngular.config;

import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.EnumSet;

@Configuration
public class CookieConfig {

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();

            // Duración de la sesión (24 horas)
            sessionCookieConfig.setMaxAge(86400);

            // HttpOnly = true → No accesible desde JavaScript (más seguro)
            sessionCookieConfig.setHttpOnly(true);

            // Secure = false → Para desarrollo local (HTTP)
            // Cambiar a true en producción con HTTPS
            sessionCookieConfig.setSecure(false);

            // Path de la cookie
            sessionCookieConfig.setPath("/");

            // Nombre de la cookie
            sessionCookieConfig.setName("APOLLO_SESSION");

            // SameSite: "Lax" para Angular en desarrollo
            // Cambiar a "None" en producción con HTTPS
            sessionCookieConfig.setAttribute("SameSite", "Lax");

            // Configurar tracking por cookie (CORREGIDO)
            servletContext.setSessionTrackingModes(
                    EnumSet.of(SessionTrackingMode.COOKIE)
            );
        };
    }

    // Configuración adicional para CORS con cookies
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:4200",
                                "http://localhost:4201",
                                "http://127.0.0.1:4200"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}