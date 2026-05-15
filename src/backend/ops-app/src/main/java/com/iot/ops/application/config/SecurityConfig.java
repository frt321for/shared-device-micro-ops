package com.iot.ops.application.config;

import com.iot.ops.application.infra.security.JwtFilter;
import com.iot.ops.application.infra.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // JWT sets authorities as ROLE_admin, ROLE_manager, etc. via JwtFilter.
    // Usage: @PreAuthorize("hasRole('admin')") on controller methods,
    // or add requestMatchers rules here, e.g.:
    //   .requestMatchers("/api/v1/admin/**").hasRole("admin")
    //   .requestMatchers(HttpMethod.DELETE, "/api/v1/sites/**").hasRole("admin")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN.getValue().toLowerCase())
                .requestMatchers(HttpMethod.POST, "/api/v1/sites/**").hasRole(Role.ADMIN.getValue().toLowerCase())
                .requestMatchers(HttpMethod.PUT, "/api/v1/sites/**").hasRole(Role.ADMIN.getValue().toLowerCase())
                .requestMatchers(HttpMethod.DELETE, "/api/v1/sites/**").hasRole(Role.ADMIN.getValue().toLowerCase())
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
