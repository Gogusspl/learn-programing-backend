package org.example;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // Jeśli nie ma tokena, po prostu przejdź dalej w łańcuchu filtrów
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = null;
        Set<Role> roles = null;

        try {
            username = jwtUtil.extractUsername(token);
            roles = jwtUtil.extractRoles(token);
        } catch (ExpiredJwtException ex) {
            // Jeśli token wygasł, ustaw status odpowiedzi na 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Możesz także dodać wiadomość do odpowiedzi, aby ułatwić debugowanie
            response.getWriter().write("JWT expired");
            return; // Przerwij łańcuch filtrów
        } catch (Exception e) {
            // Obsłuż inne potencjalne błędy walidacji tokena
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Invalid JWT");
            return; // Przerwij łańcuch filtrów
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.name()))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}