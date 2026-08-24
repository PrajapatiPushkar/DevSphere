package com.devsphere.user.security;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.service.UserProfileService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserServiceSecurityTest {

    private static final String SECRET = "devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    private SecretKey key;

    @BeforeEach
    void setUp() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        UserProfileResponse mockProfile = new UserProfileResponse(
                101L, "Pushkar", "Prajapati", "Pushkar P", "Dev", "1234567890", Instant.now(), Instant.now()
        );
        when(userProfileService.getOrCreateProfile(anyLong())).thenReturn(mockProfile);
    }

    private String createJwt(Long userId, String email, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", List.of(role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Direct access to User Service without JWT returns 401 Unauthorized")
    void directAccessWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Access /me with valid USER JWT returns 200 OK")
    void accessMeWithUserJwtReturns200() throws Exception {
        String token = createJwt(101L, "user@example.com", "USER");

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101));
    }

    @Test
    @DisplayName("USER accessing own profile by userId returns 200 OK")
    void userAccessingOwnProfileByIdReturns200() throws Exception {
        String token = createJwt(101L, "user@example.com", "USER");

        mockMvc.perform(get("/api/v1/users/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER accessing another user's profile by userId returns 403 Forbidden")
    void userAccessingAnotherUserProfileByIdReturns403() throws Exception {
        String token = createJwt(101L, "user@example.com", "USER");

        mockMvc.perform(get("/api/v1/users/102")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    @DisplayName("ADMIN accessing another user's profile by userId is allowed (200 OK)")
    void adminAccessingAnotherUserProfileByIdReturns200() throws Exception {
        String adminToken = createJwt(999L, "admin@devsphere.local", "ADMIN");

        mockMvc.perform(get("/api/v1/users/102")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER accessing admin summary endpoint returns 403 Forbidden")
    void userAccessingAdminSummaryReturns403() throws Exception {
        String token = createJwt(101L, "user@example.com", "USER");

        mockMvc.perform(get("/api/v1/users/admin/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN accessing admin summary endpoint returns 200 OK")
    void adminAccessingAdminSummaryReturns200() throws Exception {
        String adminToken = createJwt(999L, "admin@devsphere.local", "ADMIN");

        mockMvc.perform(get("/api/v1/users/admin/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.service").value("DEVSPHERE-USER-SERVICE"));
    }
}
