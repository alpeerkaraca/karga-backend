package com.alpeerkaraca.common.security;

import com.alpeerkaraca.common.exception.ExtractionException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    private final JWTService jwtService;

    public JWTAuthenticationFilter(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String username;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwtToken = getJwtFromRequest(request);
        try {
            username = jwtService.extractUsername(jwtToken);
        } catch (Exception e) {
            throw new ExtractionException("Failed to extract username from JWT token");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isTokenValid(jwtToken)) {

            String userId;
            try {
                userId = jwtService.extractUserId(jwtToken);
            } catch (Exception e) {
                throw new ExtractionException("Failed to extract ID from JWT token");
            }
            List<String> roles;
            try {
                roles = jwtService.extractRoles(jwtToken);
            } catch (Exception e) {
                throw new ExtractionException("Failed to extract roles from JWT token");
            }
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    roles.stream().map(SimpleGrantedAuthority::new).toList()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }


        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");
        return authorizationHeader.substring(7);
    }
}
