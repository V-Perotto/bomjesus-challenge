package br.com.bomjesus.academic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Acesso entre origens (FR-026a): só as origens configuradas em {@code app.cors.allowed-origins}
 * podem chamar a API diretamente. No fluxo padrão (proxy do Vite ou Nginx) não é usado.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT)
                .exposedHeaders(HttpHeaders.LOCATION);
    }
}
