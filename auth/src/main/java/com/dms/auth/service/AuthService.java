// AuthService.java
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        Set<Department> departments = departmentRepository.findByIdIn(request.getDepartmentIds());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .departments(departments)
                .build();

        User savedUser = userRepository.save(user);

        return mapUserToResponse(savedUser);
    }

    public JwtResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        return JwtResponse.builder()
                .token(jwt)
                .userId(userDetails.getId())
                .email(userDetails.getUsername())
                .role(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                .build();
    }

    public boolean validateToken(String token) {
        return jwtUtils.validateJwtToken(token);
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