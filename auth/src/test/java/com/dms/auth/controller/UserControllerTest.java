package com.dms.auth.controller;

import com.dms.auth.dto.DepartmentAssignRequest;
import com.dms.auth.dto.UserResponse;
import com.dms.auth.dto.UserUpdateRequest;
import com.dms.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_asAdmin_shouldReturnAllUsers() throws Exception {
        // Arrange
        UserResponse user1 = UserResponse.builder()
                .id(1L)
                .email("user1@example.com")
                .role("USER")
                .departments(Set.of("HR"))
                .build();

        UserResponse user2 = UserResponse.builder()
                .id(2L)
                .email("user2@example.com")
                .role("USER")
                .departments(Set.of("IT"))
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_asUser_shouldBeForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1@example.com", roles = "USER")
    void getUserById_asSameUser_shouldReturnUser() throws Exception {
        // Arrange
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("user1@example.com")
                .role("USER")
                .departments(Set.of("HR"))
                .build();

        when(userService.getUserById(1L)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user1@example.com"));
    }

    @Test
    @WithMockUser(username = "user1@example.com", roles = "USER")
    void getUserById_asDifferentUser_shouldBeForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_asAdmin_shouldReturnUser() throws Exception {
        // Arrange
        UserResponse user = UserResponse.builder()
                .id(2L)
                .email("user2@example.com")
                .role("USER")
                .departments(Set.of("IT"))
                .build();

        when(userService.getUserById(2L)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.email").value("user2@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_asAdmin_shouldUpdateUser() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("updated@example.com");
        request.setPassword("newpassword");
        request.setRole("ADMIN");
        request.setDepartmentIds(Set.of(1L, 2L));

        UserResponse updatedUser = UserResponse.builder()
                .id(1L)
                .email("updated@example.com")
                .role("ADMIN")
                .departments(Set.of("HR", "IT"))
                .build();

        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_shouldDeleteUser() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignDepartments_asAdmin_shouldAssignDepartments() throws Exception {
        // Arrange
        DepartmentAssignRequest request = new DepartmentAssignRequest();
        request.setDepartmentIds(Set.of(1L, 2L));

        UserResponse updatedUser = UserResponse.builder()
                .id(1L)
                .email("user1@example.com")
                .role("USER")
                .departments(Set.of("HR", "IT"))
                .build();

        when(userService.assignDepartmentsToUser(eq(1L), any())).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(post("/api/users/1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments").isArray())
                .andExpect(jsonPath("$.departments.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeDepartments_asAdmin_shouldRemoveDepartments() throws Exception {
        // Arrange
        DepartmentAssignRequest request = new DepartmentAssignRequest();
        request.setDepartmentIds(Set.of(1L));

        UserResponse updatedUser = UserResponse.builder()
                .id(1L)
                .email("user1@example.com")
                .role("USER")
                .departments(Set.of("IT"))
                .build();

        when(userService.removeDepartmentsFromUser(eq(1L), any())).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(delete("/api/users/1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments").isArray())
                .andExpect(jsonPath("$.departments.length()").value(1))
                .andExpect(jsonPath("$.departments[0]").value("IT"));
    }
}