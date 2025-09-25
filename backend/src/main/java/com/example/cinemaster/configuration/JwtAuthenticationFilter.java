package com.example.cinemaster.configuration;

import com.example.cinemaster.service.JwtService;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // cắt "Bearer "
        String phone = null;

        try {
            // validateToken sẽ check cả blacklist
            if (!jwtService.validateToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            phone = jwtService.extractPhone(jwt);
        } catch (Exception e) {
            // Token không hợp lệ → bỏ qua
            filterChain.doFilter(request, response);
            return;
        }

        if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Account account = accountRepository.findByPhoneNumberAndIsActiveTrue(phone).orElse(null);

            if (account != null) {
                // TODO: lấy role từ account để set authorities
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                phone, null, null // hoặc List<GrantedAuthority>
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
