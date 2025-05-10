package com.dms.auth.service;

import com.dms.auth.dto.UserResponse;
import com.dms.auth.dto.UserUpdateRequest;
import com.dms.auth.exception.ResourceNotFoundException;
import com.dms.auth.model.Department;
import com.dms.auth.model.User;
import com.dms.auth.repository.DepartmentRepository;
import com.dms.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User user;
    private Department department1;
    private Department department2;
    
    @BeforeEach
    void setUp() {
        department1 = new Department(1L, "HR");
        department2 = new Department(2L, "IT");
        
        Set<Department> departments = new HashSet<>();
        departments.add(department1);
        
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role("USER")
                .departments(departments)
                .build();
    }
    
    @Test
    void getAllUsers_shouldReturnAllUsers() {
        // Arrange
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);
        
        // Act
        List<UserResponse> result = userService.getAllUsers();
        
        // Assert
        assertEquals(1, result.size());
        UserResponse response = result.get(0);
        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals(Set.of("HR"), response.getDepartments());
    }
    
    @Test
    void getUserById_withExistingUser_shouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // Act
        UserResponse result = userService.getUserById(1L);
        
        // Assert
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("USER", result.getRole());
        assertEquals(Set.of("HR"), result.getDepartments());
    }
    
    @Test
    void getUserById_withNonExistentUser_shouldThrowException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> userService.getUserById(999L));
        
        assertEquals("User not found with id: 999", exception.getMessage());
    }
    
    @Test
    void updateUser_withValidRequest_shouldUpdateUser() {
        // Arrange
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setEmail("updated@example.com");
        updateRequest.setPassword("newPassword");
        updateRequest.setRole("ADMIN");
        updateRequest.setDepartmentIds(Set.of(1L, 2L));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");
        when(departmentRepository.findByIdIn(updateRequest.getDepartmentIds())).thenReturn(Set.of(department1, department2));
        when(userRepository.save(any(User.class))).thenReturn(user); // Updated user
        
        // Act
        UserResponse result = userService.updateUser(1L, updateRequest);
        
        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertEquals("updated@example.com", capturedUser.getEmail());
        assertEquals("newHashedPassword", capturedUser.getPasswordHash());
        assertEquals("ADMIN", capturedUser.getRole());
        assertEquals(2, capturedUser.getDepartments().size());
        assertTrue(capturedUser.getDepartments().contains(department1));
        assertTrue(capturedUser.getDepartments().contains(department2));
    }
    
    @Test
    void deleteUser_withExistingUser_shouldDeleteUser() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        
        // Act
        userService.deleteUser(1L);
        
        // Assert
        verify(userRepository).deleteById(1L);
    }
    
    @Test
    void deleteUser_withNonExistentUser_shouldThrowException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> userService.deleteUser(999L));
        
        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, never()).deleteById(any());
    }
    
    @Test
    void assignDepartmentsToUser_withValidRequest_shouldAddDepartments() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findByIdIn(Set.of(2L))).thenReturn(Set.of(department2));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Act
        UserResponse result = userService.assignDepartmentsToUser(1L, Set.of(2L));
        
        // Assert
        verify(userRepository).save(user);
        assertTrue(user.getDepartments().contains(department2));
    }
    
    @Test
    void removeDepartmentsFromUser_withValidRequest_shouldRemoveDepartments() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Act
        UserResponse result = userService.removeDepartmentsFromUser(1L, Set.of(1L));
        
        // Assert
        verify(userRepository).save(user);
        assertTrue(user.getDepartments().isEmpty());
    }
}