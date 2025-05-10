package com.dms.auth.service;

import com.dms.auth.dto.JwtResponse;
import com.dms.auth.dto.LoginRequest;
import com.dms.auth.dto.RegisterRequest;
import com.dms.auth.dto.UserResponse;
import com.dms.auth.model.Department;
import com.dms.auth.model.User;
import com.dms.auth.repository.DepartmentRepository;
import com.dms.auth.repository.UserRepository;
import com.dms.auth.security.JwtUtils;
import com.dms.auth.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtUtils jwtUtils;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private AuthService authService;
    
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Department department;
    
    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");
        registerRequest.setRole("USER");
        registerRequest.setDepartmentIds(Set.of(1L));
        
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        department = new Department(1L, "HR");
    }
    
    @Test
    void registerUser_withValidRequest_shouldCreateUser() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(departmentRepository.findByIdIn(registerRequest.getDepartmentIds())).thenReturn(Set.of(department));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .email(registerRequest.getEmail())
                .passwordHash("hashedPassword")
                .role(registerRequest.getRole())
                .departments(Set.of(department))
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        UserResponse response = authService.registerUser(registerRequest);
        
        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertEquals(registerRequest.getEmail(), capturedUser.getEmail());
        assertEquals("hashedPassword", capturedUser.getPasswordHash());
        assertEquals(registerRequest.getRole(), capturedUser.getRole());
        assertTrue(capturedUser.getDepartments().contains(department));
        
        assertEquals(1L, response.getId());
        assertEquals(registerRequest.getEmail(), response.getEmail());
        assertEquals(registerRequest.getRole(), response.getRole());
        assertEquals(Set.of("HR"), response.getDepartments());
    }
    
    @Test
    void registerUser_withExistingEmail_shouldThrowException() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.registerUser(registerRequest));
        
        assertEquals("Email is already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void authenticateUser_withValidCredentials_shouldReturnJwtToken() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        
        String jwtToken = "generatedToken";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn(jwtToken);
        
        // Act
        JwtResponse response = authService.authenticateUser(loginRequest);
        
        // Assert
        assertEquals(jwtToken, response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
    }
    
    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        // Arrange
        String token = "validToken";
        when(jwtUtils.validateJwtToken(token)).thenReturn(true);
        
        // Act
        boolean result = authService.validateToken(token);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        // Arrange
        String token = "invalidToken";
        when(jwtUtils.validateJwtToken(token)).thenReturn(false);
        
        // Act
        boolean result = authService.validateToken(token);
        
        // Assert
        assertFalse(result);
    }
}