package com.pharmacy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Called when unauthenticated user tries to access protected resource
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        sendErrorResponse(response, HttpStatus.UNAUTHORIZED,
                "Authentication required",
                authException.getMessage(),
                request.getRequestURI());
    }

    // Called when authenticated user doesn't have required role
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        sendErrorResponse(response, HttpStatus.FORBIDDEN,
                "Access denied",
                "You don't have permission to access this resource",
                request.getRequestURI());
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   String error,
                                   String message,
                                   String path) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", path);

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}