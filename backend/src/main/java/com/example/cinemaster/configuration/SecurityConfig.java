package com.example.cinemaster.configuration;

import com.example.cinemaster.service.CustomOAuth2UserService;
import com.example.cinemaster.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          CustomOAuth2UserService customOAuth2UserService) {
        this.customUserDetailsService = customUserDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/demo/login",
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/demo/oauth2/**",
                                "/demo/login/oauth2/**",
                                "/demo/static/**",
                                "/demo/public/**",
                                "/api/auth/me" // ✅ FE sẽ gọi API này để lấy user info
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // ✅ Form login trả JSON thay vì redirect
                .formLogin(form -> form
                        .loginProcessingUrl("/demo/login")
                        .successHandler((request, response, authentication) -> {
                            String username = authentication.getName();
//                            response.sendRedirect("http://localhost:63342/CineMaster/frontend/user/profile.html?username=" + username);
                            response.sendRedirect("http://localhost:63342/CineMaster/frontend/user/profile.html");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":\"error\", \"message\":\"Sai tài khoản hoặc mật khẩu\"}");
                        })
                        .permitAll()
                )

                // ✅ Google OAuth2 cũng trả JSON
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/demo/login")
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userService(customOAuth2UserService)
                                )
                                .successHandler((request, response, authentication) -> {
                                    String username = authentication.getName();

                                    response.sendRedirect("http://localhost:63342/CineMaster/frontend/user/profile.html");

                                })
                                .failureHandler((request, response, exception) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write("{\"status\":\"error\", \"message\":\"Login with Google failed\"}");
                                })
                )


                // ✅ Logout trả JSON
                .logout(logout -> logout
                        .logoutUrl("/demo/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":\"logged_out\"}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                );

        return http.build();
    }
}
