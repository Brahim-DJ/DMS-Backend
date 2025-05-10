package com.dms.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtils jwtUtils;
    
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;
    
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void doFilterInternal_withValidToken_shouldSetAuthentication() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        String email = "user@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateJwtToken(token)).thenReturn(true);
        when(jwtUtils.getUsernameFromJwtToken(token)).thenReturn(email);
        
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(1L)
                .email(email)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .departments(new HashSet<>())
                .build();
        
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        
        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_withNoToken_shouldNotSetAuthentication() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, never()).validateJwtToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_withInvalidToken_shouldNotSetAuthentication() throws Exception {
        // Arrange
        String token = "invalid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateJwtToken(token)).thenReturn(false);
        
        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, never()).getUsernameFromJwtToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_withExceptionThrown_shouldNotSetAuthenticationAndContinueChain() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateJwtToken(token)).thenThrow(new RuntimeException("Test exception"));
        
        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}