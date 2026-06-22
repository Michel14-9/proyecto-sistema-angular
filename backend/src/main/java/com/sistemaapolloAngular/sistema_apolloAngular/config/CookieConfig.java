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
            sessionCookieConfig.setSecure(false);

            // Path de la cookie
            sessionCookieConfig.setPath("/");

            // ✅ Nombre de la cookie - USAR JSESSIONID (estándar de Spring Security)
            sessionCookieConfig.setName("JSESSIONID");

            // SameSite: "Lax" para Angular en desarrollo
            sessionCookieConfig.setAttribute("SameSite", "Lax");

            // Configurar tracking por cookie
            servletContext.setSessionTrackingModes(
                    EnumSet.of(SessionTrackingMode.COOKIE)
            );
        };
    }

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