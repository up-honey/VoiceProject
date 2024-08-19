package com.voice.app.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;

import com.voice.app.exception.CustomAccessDeniedHandler;
import com.voice.app.jwt.CustomLogoutFilter;
import com.voice.app.jwt.JWTFilter;
import com.voice.app.jwt.JWTUtil;
import com.voice.app.jwt.LoginFilter;
import com.voice.app.repository.RefreshRepository;

import lombok.RequiredArgsConstructor;


@Configuration
//@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/css/**", "/js/**", "/images/**");
    }
    
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<AntPathRequestMatcher> permitAllMatchers = Arrays.asList(
        );

        http
        .csrf(csrf -> csrf.disable())
        .formLogin(formLogin -> formLogin.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
            .requestMatchers("/login", "/signup", "/member/login", "/member/sign-up", "/api/**", "/member/**").permitAll()
            .requestMatchers("/reissue").permitAll()
            .requestMatchers("/validate-token").permitAll()
            .requestMatchers("/checkLogin").permitAll()
            .requestMatchers("/error/**").permitAll()
            .requestMatchers("/admin/**").hasAnyRole("ADMIN")
            
            .anyRequest().authenticated()
        )
        .exceptionHandling(exceptionHandling -> exceptionHandling
        	    .accessDeniedHandler((request, response, accessDeniedException) -> {
        	        response.sendError(HttpStatus.FORBIDDEN.value());
        	    })
        	    .authenticationEntryPoint((request, response, authException) -> {
        	        response.sendError(HttpStatus.UNAUTHORIZED.value());
        	    })
        	)
        .addFilterBefore(new JWTFilter(jwtUtil, permitAllMatchers), UsernamePasswordAuthenticationFilter.class)
        .addFilter(new LoginFilter(authenticationManager(http.getSharedObject(AuthenticationConfiguration.class)), jwtUtil, refreshRepository))
        .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), LogoutFilter.class);

    return http.build();
    }
}
