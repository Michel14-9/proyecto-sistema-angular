package com.sistemaapolloAngular.sistema_apolloAngular.config;

import jakarta.servlet.SessionCookieConfig;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieConfig {

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();


            sessionCookieConfig.setMaxAge(-1);
            sessionCookieConfig.setHttpOnly(true);
            sessionCookieConfig.setSecure(false);
            sessionCookieConfig.setPath("/");
            sessionCookieConfig.setName("JSESSIONID");


            sessionCookieConfig.setAttribute("SameSite", "Strict");
        };
    }
}
