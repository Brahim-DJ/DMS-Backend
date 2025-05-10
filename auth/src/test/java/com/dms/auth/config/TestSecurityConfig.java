package com.dms.auth.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // For tests, we can configure a simplified security chain
        http.csrf().disable()
            .authorizeHttpRequests(authorize -> 
                authorize.requestMatchers("/api/auth/**").permitAll()
                         .anyRequest().authenticated());
        
        return http.build();
    }
}