package com.dms.auth.integration;

import com.dms.auth.config.TestConfig;
import com.dms.auth.dto.DepartmentAssignRequest;
import com.dms.auth.dto.JwtResponse;
import com.dms.auth.dto.LoginRequest;
import com.dms.auth.dto.UserUpdateRequest;
import com.dms.auth.model.Department;
import com.dms.auth.model.User;
import com.dms.auth.repository.DepartmentRepository;
import com.dms.auth.repository.UserRepository;
import com.dms.auth.service.AuthService;
import com.dms.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DepartmentRepository departmentRepository;
    
    @MockBean
    private UserService userService;
    
    @MockBean
    private AuthService authService;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminShouldPerformFullUserManagementCycle() throws Exception {
        // Mock userService responses for each operation
        
        // Mock getAllUsers
        when(userService.getAllUsers()).thenReturn(Arrays.asList(
            com.dms.auth.dto.UserResponse.builder()
                .id(1L)
                .email("admin@example.com")
                .role("ADMIN")
                .departments(Set.of("HR", "IT"))
                .build(),
            com.dms.auth.dto.UserResponse.builder()
                .id(2L)
                .email("user@example.com")
                .role("USER")
                .departments(Set.of("HR"))
                .build()
        ));
        
        // Mock getUserById
        when(userService.getUserById(2L)).thenReturn(
            com.dms.auth.dto.UserResponse.builder()
                .id(2L)
                .email("user@example.com")
                .role("USER")
                .departments(Set.of("HR"))
                .build()
        );
        
        // Mock updateUser
        when(userService.updateUser(anyLong(), any())).thenReturn(
            com.dms.auth.dto.UserResponse.builder()
                .id(2L)
                .email("updated@example.com")
                .role("MANAGER")
                .departments(Set.of("HR"))
                .build()
        );
        
        // Mock assignDepartmentsToUser
        when(userService.assignDepartmentsToUser(anyLong(), anySet())).thenReturn(
            com.dms.auth.dto.UserResponse.builder()
                .id(2L)
                .email("updated@example.com")
                .role("MANAGER")
                .departments(Set.of("HR", "IT"))
                .build()
        );
        
        // Mock removeDepartmentsFromUser
        when(userService.removeDepartmentsFromUser(anyLong(), anySet())).thenReturn(
            com.dms.auth.dto.UserResponse.builder()
                .id(2L)
                .email("updated@example.com")
                .role("MANAGER")
                .departments(Set.of("HR"))
                .build()
        );
        
        // Mock deleteUser
        doNothing().when(userService).deleteUser(anyLong());
        
        // 1. List all users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        
        // 2. Get single user
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
        
        // 3. Update user
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setEmail("updated@example.com");
        updateRequest.setRole("MANAGER");
        
        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
        
        // 4. Add department to user
        DepartmentAssignRequest assignRequest = new DepartmentAssignRequest();
        assignRequest.setDepartmentIds(Set.of(2L));
        
        mockMvc.perform(post("/api/users/2/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments", hasSize(2)));
        
        // 5. Remove department from user
        mockMvc.perform(delete("/api/users/2/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments", hasSize(1)))
                .andExpect(jsonPath("$.departments[0]").value("HR"));
        
        // 6. Delete user
        mockMvc.perform(delete("/api/users/2"))
                .andExpect(status().isNoContent());
    }
}