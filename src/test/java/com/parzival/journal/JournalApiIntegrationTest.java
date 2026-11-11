package com.parzival.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parzival.journal.auth.resource.LoginRequest;
import com.parzival.journal.auth.service.JwtService;
import com.parzival.journal.entity.JournalEntity;
import com.parzival.journal.entity.Role;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.repository.JournalRepository;
import com.parzival.journal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JournalApiIntegrationTest {

    private static final String PASSWORD = "StrongPass123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        journalRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationAndLoginShouldWork() throws Exception {
        String registerBody = """
                {
                  "email": "new.user@example.com",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String loginBody = """
                {
                  "email": "NEW.USER@example.com",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registrationShouldRejectInvalidEmailAndShortPassword() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldRejectDisabledUser() throws Exception {
        UserEntity disabled = createUser("disabled@example.com", Role.USER, false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(disabled.getEmail(), PASSWORD)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestsShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/journals"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserShouldNotAccessAdminEndpoints() throws Exception {
        UserEntity user = createUser("member@example.com", Role.USER, true);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldAccessAdminEndpoints() throws Exception {
        createUser("member@example.com", Role.USER, true);
        UserEntity admin = createUser("admin@example.com", Role.ADMIN, true);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[?(@.email == 'admin@example.com')].passwordHash").doesNotExist());
    }

    @Test
    void userMustNotReadAnotherUsersJournal() throws Exception {
        UserEntity owner = createUser("owner@example.com", Role.USER, true);
        UserEntity otherUser = createUser("other@example.com", Role.USER, true);
        JournalEntity journal = createJournal(owner, "Private entry", "This is private");

        mockMvc.perform(get("/api/journals/{id}", journal.getId())
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void journalListMustOnlyContainAuthenticatedUsersEntries() throws Exception {
        UserEntity firstUser = createUser("first@example.com", Role.USER, true);
        UserEntity secondUser = createUser("second@example.com", Role.USER, true);
        createJournal(firstUser, "First user's note", "alpha private content");
        createJournal(secondUser, "Second user's note", "beta private content");

        mockMvc.perform(get("/api/journals")
                        .param("search", "private")
                        .header("Authorization", bearer(firstUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("First user's note"));
    }

    @Test
    void disabledUserMustNotUsePreviouslyIssuedJwt() throws Exception {
        UserEntity user = createUser("will.disable@example.com", Role.USER, true);
        String token = bearer(user);
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/journals")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void journalValidationShouldReturnBadRequest() throws Exception {
        UserEntity user = createUser("writer@example.com", Role.USER, true);
        String body = """
                {
                  "title": "   ",
                  "content": "Some content"
                }
                """;

        mockMvc.perform(post("/api/journals")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidJournalDateRangeShouldReturnBadRequest() throws Exception {
        UserEntity user = createUser("date-filter@example.com", Role.USER, true);

        mockMvc.perform(get("/api/journals")
                        .param("fromDate", "2026-11-12T00:00:00Z")
                        .param("toDate", "2026-11-11T00:00:00Z")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
    }

    private UserEntity createUser(String email, Role role, boolean enabled) {
        return userRepository.save(UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(role)
                .enabled(enabled)
                .build());
    }

    private JournalEntity createJournal(UserEntity user, String title, String content) {
        Instant now = Instant.now();
        return journalRepository.save(JournalEntity.builder()
                .user(user)
                .title(title)
                .content(content)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private String bearer(UserEntity user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
