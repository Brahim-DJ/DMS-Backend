package com.dms.auth.security;

import com.dms.auth.model.User;
import com.dms.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_withExistingUser_shouldReturnUserDetails() {
        // Arrange
        String email = "test@example.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .passwordHash("hashedPassword")
                .role("USER")
                .departments(new HashSet<>())
                .build();
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        // Assert
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_withNonExistentUser_shouldThrowException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, 
            () -> userDetailsService.loadUserByUsername(email));
        
        assertEquals("User not found with email: " + email, exception.getMessage());
        verify(userRepository, times(1)).findByEmail(email);
    }
}