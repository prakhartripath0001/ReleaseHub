
package com.yourapp.catalog.common;

import com.yourapp.catalog.common.security.RoleCheckFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleCheckFilterTest {

    private RoleCheckFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RoleCheckFilter();
    }

    @Test
    void doFilter_shouldAllowGetRequestsWithoutRole() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldAllowPostWithXRolePublisher() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Role")).thenReturn("PUBLISHER");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldAllowPostWithXUserRolePublisher() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Role")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("PUBLISHER");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldAllowPostWithAdminRole() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Role")).thenReturn("ADMIN");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldBlockPostWithoutRole() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Role")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(stringWriter.toString()).contains("Forbidden");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_shouldBlockPutWithInvalidRole() throws Exception {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getHeader("X-Role")).thenReturn("USER");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }
}
