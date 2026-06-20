package com.sistemaapolloAngular.sistema_apolloAngular.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class AdditionalSecurityConfig {

    /**
     * Filtro para manejar correctamente los headers en proxies/reverse proxies
     *
     * Este filtro es necesario cuando:
     * - Usas Nginx, Apache o Cloudflare como proxy
     * - Tu aplicación está detrás de un balanceador de carga
     * - Necesitas obtener la IP real del cliente
     * - HTTPS termina en el proxy y la app usa HTTP internamente
     */
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> registrationBean =
                new FilterRegistrationBean<>();

        // Crear el filtro
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

        // Configurar el filtro
        registrationBean.setFilter(filter);

        // Aplicar a todas las rutas
        registrationBean.addUrlPatterns("/*");

        // Establecer orden (prioridad alta para procesar antes que otros filtros)
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        // Nombre del filtro (opcional)
        registrationBean.setName("forwardedHeaderFilter");

        return registrationBean;
    }
}