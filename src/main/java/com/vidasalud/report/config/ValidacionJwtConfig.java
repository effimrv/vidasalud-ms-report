package com.vidasalud.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

/**
 * Filtro de validación del JWT de Microsoft Entra ID: firma, vigencia, emisor del tenant (iss)
 * y audiencia de la API (aud). Un token de otro tenant o de otra aplicación se rechaza con 401.
 */
@Configuration
public class ValidacionJwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${vidasalud.jwt.emisores}") List<String> emisores,
            @Value("${vidasalud.jwt.audiencias}") List<String> audiencias) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(validador(emisores, audiencias));
        return decoder;
    }

    /** Vigencia (exp/nbf) + emisor del tenant + audiencia de la API. */
    static OAuth2TokenValidator<Jwt> validador(List<String> emisores, List<String> audiencias) {
        OAuth2TokenValidator<Jwt> emisor = new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                iss -> iss != null && emisores.contains(iss.toString()));
        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                aud -> aud != null && aud.stream().anyMatch(a -> audiencias.contains(a)));
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), emisor, audiencia);
    }
}
