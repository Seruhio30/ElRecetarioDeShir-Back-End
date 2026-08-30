package com.elrecetariodeshir.backend.admin.auth;

public record AdminAuthErrorResponse(
        String code,
        String message) {
}
