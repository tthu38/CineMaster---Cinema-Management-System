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
import org.springframework.util.AntPathMatcher;
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
    //thêm ở đây
//    private final String[] PUBLIC_ENDPOINTS = {
//            "/api/v1/auth/**",
//            "/api/v1/password/**",
//            "/uploads/**",
//            "/api/v1/branches/**",
//            "/api/v1/auditoriums/**",
//            "/api/v1/seats/**",      // <--- THÊM
//            "/api/v1/seattypes/**",
//            "/api/v1/screening-periods/**",
//    };
    private final String[] PUBLIC_ENDPOINTS_FOR_FILTER = {
            "/api/v1/auth/**",
            "/api/v1/password/**",
            "/uploads/**",
            "/api/v1/branches/**",
            "/api/v1/auditoriums/**",
            "/api/v1/seats/**",
            "/api/v1/seattypes/**",
            "/api/v1/screening-periods/**",
    };

//    private static final org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();
private final AntPathMatcher pathMatcher = new AntPathMatcher();
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Kiểm tra xem request path có khớp với bất kỳ endpoint public nào không
//        String path = request.getRequestURI();

        // Dùng Spring AntPathMatcher để so khớp các patterns có "**"
        // Vì Spring không cung cấp sẵn AntPathMatcher, ta dùng cách đơn giản hóa:

        // Đây là giải pháp tạm thời đơn giản, nếu không có AntPathMatcher
//        for (String endpoint : PUBLIC_ENDPOINTS) {
//            // Loại bỏ "**" và so khớp cơ bản (chỉ để kiểm tra Branchs và Auditoriums)
//            if (path.startsWith(endpoint.replace("/**", ""))) {
//                return true;
//            }
//        }

        // Cách tốt hơn là sử dụng AntPathMatcher của Spring nếu bạn inject được
        // Hoặc kiểm tra chính xác các path cần bỏ qua:
//        if (path.startsWith("/api/v1/branches") || path.startsWith("/api/v1/auditoriums")|| path.startsWith("/api/v1/seats")         // <--- THÊM
//                || path.startsWith("/api/v1/seattypes")) {
//            return true;
//        }
        String path = request.getRequestURI();

        for (String endpoint : PUBLIC_ENDPOINTS_FOR_FILTER) {
            // Dùng pathMatcher.match để so khớp chính xác patterns **
            if (pathMatcher.match(endpoint, path)) {
                return true;
            }
        }



        return false;
    }
// kết thúc
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
