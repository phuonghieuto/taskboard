package com.phuonghieuto.backend.auth_service.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.phuonghieuto.backend.auth_service.filter.CustomBearerTokenAuthenticationFilter;
import com.phuonghieuto.backend.auth_service.security.CustomAuthenticationEntryPoint;
import com.phuonghieuto.backend.auth_service.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.phuonghieuto.backend.auth_service.security.OAuth2AuthenticationFailureHandler;
import com.phuonghieuto.backend.auth_service.security.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final DefaultOAuth2UserService oAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

        @Bean
        protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
                return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
        }

        @Bean
        public SecurityFilterChain filterChain(final HttpSecurity httpSecurity,
                        final CustomBearerTokenAuthenticationFilter customBearerTokenAuthenticationFilter,
                        final CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                        final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository)
                        throws Exception {

                httpSecurity.exceptionHandling(
                                customizer -> customizer.authenticationEntryPoint(customAuthenticationEntryPoint))
                                .cors(customizer -> customizer.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(customizer -> customizer
                                                .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/users/**").permitAll()
                                                .requestMatchers("/auth/api-docs/**", "/auth/swagger-ui.html/**",
                                                                "/auth/swagger-ui/**")
                                                .permitAll().requestMatchers(HttpMethod.GET, "/auth/authenticate")
                                                .permitAll().requestMatchers(HttpMethod.GET, "/users/*/email")
                                                .permitAll().requestMatchers(HttpMethod.GET, "/users/by-email")
                                                .permitAll().requestMatchers(HttpMethod.GET, "/users/confirm-email")
                                                .permitAll().requestMatchers("/oauth2/**").permitAll()
                                                .requestMatchers("/login-page").permitAll()
                                                .requestMatchers("/login/oauth2/code/*").permitAll().anyRequest()
                                                .authenticated())
                                .sessionManagement(customizer -> customizer
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(customBearerTokenAuthenticationFilter,
                                                BearerTokenAuthenticationFilter.class)
                                .oauth2Login(oauth2 -> oauth2.loginPage("/login-page")
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorize")
                                                                .authorizationRequestRepository(
                                                                                cookieAuthorizationRequestRepository))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler));
                return httpSecurity.build();
        }

        private CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("*"));
                configuration.setAllowedMethods(List.of("*"));
                configuration.setAllowedHeaders(List.of("*"));
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}