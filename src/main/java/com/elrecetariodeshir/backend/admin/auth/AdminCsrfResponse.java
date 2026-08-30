package com.elrecetariodeshir.backend.admin.auth;

public record AdminCsrfResponse(
        String headerName,
        String parameterName,
        String token) {
}
