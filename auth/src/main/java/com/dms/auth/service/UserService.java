package com.dms.auth.service;

import com.dms.auth.dto.UserResponse;
import com.dms.auth.dto.UserUpdateRequest;
import com.dms.auth.exception.ResourceNotFoundException;
import com.dms.auth.model.Department;
import com.dms.auth.model.User;
import com.dms.auth.repository.DepartmentRepository;
import com.dms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToResponse(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Update basic fields
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            user.setRole(request.getRole());
        }

        // Update departments if provided
        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            Set<Department> departments = departmentRepository.findByIdIn(request.getDepartmentIds());
            user.setDepartments(departments);
        }

        User updatedUser = userRepository.save(user);
        return mapUserToResponse(updatedUser);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignDepartmentsToUser(Long userId, Set<Long> departmentIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Set<Department> departments = departmentRepository.findByIdIn(departmentIds);
        user.getDepartments().addAll(departments);
        
        User updatedUser = userRepository.save(user);
        return mapUserToResponse(updatedUser);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse removeDepartmentsFromUser(Long userId, Set<Long> departmentIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.getDepartments().removeIf(department -> departmentIds.contains(department.getId()));
        
        User updatedUser = userRepository.save(user);
        return mapUserToResponse(updatedUser);
    }

    private UserResponse mapUserToResponse(User user) {
        Set<String> departmentNames = user.getDepartments().stream()
                .map(Department::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .departments(departmentNames)
                .build();
    }
}