package com.hszadkowski.iwa_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
@EnableMethodSecurity()
public class SecurityDevConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .sessionManagement(smc -> smc.invalidSessionUrl("/invalidSession").maximumSessions(1))
                                .requiresChannel(rcf -> rcf.anyRequest().requiresSecure()) // Only HTTPS traffic
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/h2-console/**", "/register", "/register/facebook",
                                                                "/api/auth/**", "/user-already-exist",
                                                                "/invalidSession")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/h2-console/**", "/register",
                                                                "/register/facebook", "/api/auth/**",
                                                                "/user-already-exist", "/invalidSession"))
                                .headers(headers -> headers
                                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                                .httpBasic(Customizer.withDefaults())
                                .formLogin(Customizer.withDefaults());
                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }
}
