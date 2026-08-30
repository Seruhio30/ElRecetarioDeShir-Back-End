package com.elrecetariodeshir.backend.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminSecurityErrorHandlers
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        write(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "ADMIN_AUTHENTICATION_REQUIRED",
                "Admin authentication required.");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException {

        write(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "ADMIN_ACCESS_DENIED",
                "Admin access denied.");
    }

    private void write(
            HttpServletResponse response,
            int status,
            String code,
            String message)
            throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {"code":"%s","message":"%s"}
                """.formatted(code, message).trim());
    }
}
