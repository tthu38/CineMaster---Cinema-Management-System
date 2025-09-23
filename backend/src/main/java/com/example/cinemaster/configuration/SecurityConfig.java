package com.example.cinemaster.configuration;

import com.example.cinemaster.service.CustomOAuth2UserService;
import com.example.cinemaster.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          CustomOAuth2UserService customOAuth2UserService) {
        this.customUserDetailsService = customUserDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    // Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider để Spring Security dùng CustomUserDetailsService
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // AuthenticationManager (dùng khi login programmatically)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/demo/login",
                                "/login",
                                "/register",
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/demo/oauth2/**",
                                "/demo/login/oauth2/**",
                                "/demo/static/**",
                                "/demo/public/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/",
                                "/api/auth/me"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Form login trả JSON hoặc redirect
                .formLogin(form -> form
                        .loginPage("/login") // fallback login page
                        .loginProcessingUrl("/demo/login")
                        .defaultSuccessUrl("/profile", true)
                        .successHandler((request, response, authentication) -> {
                            // FE redirect
                            response.sendRedirect("http://localhost:63342/CineMaster/frontend/user/profile.html");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":\"error\",\"message\":\"Sai tài khoản hoặc mật khẩu\"}");
                        })
                        .permitAll()
                )

                // OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/demo/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            response.sendRedirect("http://localhost:63342/CineMaster/frontend/user/profile.html");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":\"error\",\"message\":\"Login with Google failed\"}");
                        })
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/demo/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":\"logged_out\"}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }
}
