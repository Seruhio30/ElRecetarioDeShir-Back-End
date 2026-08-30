package com.elrecetariodeshir.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.media.storage.root=${java.io.tmpdir}/elrecetariodeshir-admin-security-test-media")
@AutoConfigureMockMvc
@Transactional
class AdminSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        adminUserRepository.deleteAll();

        adminUserRepository.saveAndFlush(
                new AdminUser(
                        "sergio",
                        passwordEncoder.encode("correct-password"),
                        true));

        adminUserRepository.saveAndFlush(
                new AdminUser(
                        "disabled-admin",
                        passwordEncoder.encode("correct-password"),
                        false));
    }

    @Test
    void loginCreatesAuthenticatedSession() throws Exception {
        MvcResult result = login("sergio", "correct-password");

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/admin/auth/session")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("sergio"));
    }

    @Test
    void rejectsInvalidCredentialsNeutrally() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "sergio",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid admin credentials."));

        mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "does-not-exist",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid admin credentials."));
    }

    @Test
    void rejectsDisabledAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "disabled-admin",
                                  "password": "correct-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_FAILED"));
    }

    @Test
    void rejectsAdminEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/recipes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsAuthenticatedUserWithoutAdminAuthority() throws Exception {
        mockMvc.perform(get("/api/admin/recipes")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("regular-user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_ACCESS_DENIED"));
    }

    @Test
    void publicRecipeApiRemainsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk());
    }

    @Test
    void exposesCsrfTokenForInitialFrontendHandshake() throws Exception {
        mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void csrfIsRequiredForLoginMutation() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "sergio",
                                  "password": "correct-password"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_ACCESS_DENIED"));
    }

    @Test
    void validCsrfAllowsLoginMutation() throws Exception {
        login("sergio", "correct-password");
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        MvcResult loginResult = login("sergio", "correct-password");

        MockHttpSession session =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/admin/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/auth/session")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginProtectsAgainstSessionFixation() throws Exception {
        MockHttpSession existingSession = new MockHttpSession();
        String originalSessionId = existingSession.getId();

        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .session(existingSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "sergio",
                                  "password": "correct-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) result.getRequest().getSession(false);

        assertThat(authenticatedSession).isNotNull();
        assertThat(authenticatedSession.getId())
                .isNotEqualTo(originalSessionId);
    }

    private MvcResult login(String username, String password)
            throws Exception {

        return mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();
    }
}
