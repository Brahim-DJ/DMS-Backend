package com.dms.auth.controller;

import com.dms.auth.config.WebMvcTestConfig;
import com.dms.auth.dto.JwtResponse;
import com.dms.auth.dto.LoginRequest;
import com.dms.auth.dto.RegisterRequest;
import com.dms.auth.dto.UserResponse;
import com.dms.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(WebMvcTestConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void registerUser_withValidRequest_shouldReturnCreatedUser() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");
        registerRequest.setRole("USER");
        registerRequest.setDepartmentIds(Set.of(1L));

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .role("USER")
                .departments(Set.of("HR"))
                .build();

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginUser_withValidCredentials_shouldReturnJwtToken() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        JwtResponse jwtResponse = JwtResponse.builder()
                .token("jwt.token.string")
                .userId(1L)
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(jwtResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.string"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() throws Exception {
        // Arrange
        String token = "valid.token";
        when(authService.validateToken(token)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() throws Exception {
        // Arrange
        String token = "invalid.token";
        when(authService.validateToken(token)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}