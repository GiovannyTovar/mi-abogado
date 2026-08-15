package com.mi.abogado.shared.security;

import com.mi.abogado.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Middleware de autenticacion y de resolucion de tenant.
 * <p>
 * Es el unico punto donde se escribe el {@link TenantContext}: si el token no
 * trae firma, la peticion se ejecuta sin tenant y las entidades tenant-scoped
 * no devuelven nada.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            bearerToken(request)
                    .flatMap(jwtService::verify)
                    .ifPresent(this::authenticate);
            chain.doFilter(request, response);
        } finally {
            // Imprescindible: el hilo vuelve al pool y no puede arrastrar el tenant anterior.
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(AuthPrincipal principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (principal.tenantId() != null) {
            TenantContext.set(principal.tenantId());
        }
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith(BEARER)
                ? Optional.of(header.substring(BEARER.length()))
                : Optional.empty();
    }
}
