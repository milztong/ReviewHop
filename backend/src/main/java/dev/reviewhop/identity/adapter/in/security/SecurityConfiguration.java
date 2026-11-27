package dev.reviewhop.identity.adapter.in.security;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(ApiRateLimitProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            ApiRateLimitProperties rateLimitProperties,
            Clock clock,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) throws Exception {
        var rateLimitFilter = new AuthenticatedRateLimitFilter(
                rateLimitProperties, clock, objectMapper, meterRegistry);
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/system/status",
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(realmAndScopeAuthorities());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> realmAndScopeAuthorities() {
        var scopes = new JwtGrantedAuthoritiesConverter();
        return jwt -> {
            var authorities = new ArrayList<GrantedAuthority>();
            var scopeAuthorities = scopes.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }
            return authorities;
        };
    }
}
