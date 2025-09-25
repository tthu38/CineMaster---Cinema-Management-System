package com.example.cinemaster.configuration;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

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

        String jwt = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(jwt)) {
                throw new RuntimeException("Token không hợp lệ hoặc đã bị logout");
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token hết hạn, vui lòng đăng nhập lại\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token không hợp lệ\"}");
            return;
        }

        // Lấy accountId, phone và role từ token
        Integer accountId = jwtService.extractAccountId(jwt);
        String phone = jwtService.extractPhone(jwt);
        String roleName = jwtService.extractRole(jwt);

        if (accountId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Truy vấn DB để check account có tồn tại và active
            Account account = accountRepository.findById(accountId).orElse(null);

            if (account != null && Boolean.TRUE.equals(account.getIsActive())) {
                var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(account, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
