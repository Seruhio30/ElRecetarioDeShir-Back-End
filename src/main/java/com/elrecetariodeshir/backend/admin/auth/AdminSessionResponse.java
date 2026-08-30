package com.elrecetariodeshir.backend.admin.auth;

public record AdminSessionResponse(
        boolean authenticated,
        String username) {
}
