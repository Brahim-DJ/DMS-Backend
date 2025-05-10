package com.dms.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        // Use a very long secret key to avoid WeakKeyException
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", 
                "thisisaverylongsecretkeyforhmacshaencryptionthatmustbeatleast64bitslongfortesting");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60000); // 1 minute
        
        // Create user details
        userDetails = UserDetailsImpl.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .departments(new HashSet<>())
                .build();
        
        // Only set up the mock when needed (in the tests that use it)
    }

    @Test
    void generateJwtToken_shouldReturnValidToken() {
        // Setup
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // Act
        String token = jwtUtils.generateJwtToken(authentication);
        
        // Assert
        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("test@example.com", jwtUtils.getUsernameFromJwtToken(token));
    }

    @Test
    void validateJwtToken_withValidToken_shouldReturnTrue() {
        // Setup
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtUtils.generateJwtToken(authentication);
        
        // Act & Assert
        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_withInvalidToken_shouldReturnFalse() {
        // Act & Assert
        assertFalse(jwtUtils.validateJwtToken("invalidToken"));
    }

    @Test
    void validateJwtToken_withExpiredToken_shouldReturnFalse() throws Exception {
        // Setup
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // Arrange
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1); // Expire immediately
        String token = jwtUtils.generateJwtToken(authentication);
        Thread.sleep(10); // Ensure token expires
        
        // Act & Assert
        assertFalse(jwtUtils.validateJwtToken(token));
    }

    @Test
    void getUsernameFromJwtToken_shouldExtractCorrectUsername() {
        // Setup
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // Arrange
        String token = jwtUtils.generateJwtToken(authentication);
        
        // Act
        String username = jwtUtils.getUsernameFromJwtToken(token);
        
        // Assert
        assertEquals("test@example.com", username);
    }
}