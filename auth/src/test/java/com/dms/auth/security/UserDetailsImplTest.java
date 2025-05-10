package com.dms.auth.security;

import com.dms.auth.model.Department;
import com.dms.auth.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsImplTest {

    @Test
    void build_shouldCreateUserDetailsCorrectly() {
        // Arrange
        Department department = new Department(1L, "HR");
        Set<Department> departments = new HashSet<>();
        departments.add(department);
        
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role("USER")
                .departments(departments)
                .build();
        
        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        
        // Assert
        assertEquals(1L, userDetails.getId());
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("test@example.com", userDetails.getEmail());
        assertEquals("hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
        assertEquals(departments, userDetails.getDepartments());
        
        // Check authorities
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void userDetailsImpl_shouldHaveCorrectEqualityBehavior() {
        // Arrange
        UserDetailsImpl userDetails1 = UserDetailsImpl.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .build();
        
        UserDetailsImpl userDetails2 = UserDetailsImpl.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .build();
        
        UserDetailsImpl userDetails3 = UserDetailsImpl.builder()
                .id(2L)
                .email("another@example.com")
                .password("password")
                .build();
        
        // Act & Assert
        assertEquals(userDetails1, userDetails1); // Same object
        assertNotEquals(userDetails1, userDetails3); // Different IDs
        assertNotEquals(userDetails1, null); // Against null
        assertNotEquals(userDetails1, new Object()); // Different types
    }
}