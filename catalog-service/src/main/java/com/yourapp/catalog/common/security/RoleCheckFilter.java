package com.yourapp.catalog.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Role check filter for write operations.
 * Requires X-Role header containing PUBLISHER or ADMIN for POST, PUT, DELETE requests.
 */
@Component
public class RoleCheckFilter implements Filter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE");
    private static final Set<String> ALLOWED_ROLES = Set.of("PUBLISHER", "ADMIN");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (WRITE_METHODS.contains(req.getMethod())) {
            String role = req.getHeader("X-Role");
            if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                res.getWriter().write("""
                        {"status":403,"error":"Forbidden","message":"Insufficient role"}""");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

