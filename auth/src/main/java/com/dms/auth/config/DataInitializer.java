package com.dms.auth.config;

import com.dms.auth.model.Department;
import com.dms.auth.model.User;
import com.dms.auth.repository.DepartmentRepository;
import com.dms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        // Create departments if none exist
        // if (departmentRepository.count() == 0) {
        //     departmentRepository.saveAll(List.of(
        //         new Department(null, "HR"),
        //         new Department(null, "Finance"),
        //         new Department(null, "IT"),
        //         new Department(null, "Marketing"),
        //         new Department(null, "Operations")
        //     ));
        // }
        
        // Create admin user if it doesn't exist
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123")); // Fresh hash generation
            admin.setRole("ADMIN");
            
            // Add all departments to admin
            Set<Department> allDepartments = new HashSet<>(departmentRepository.findAll());
            admin.setDepartments(allDepartments);
            
            userRepository.save(admin);
            
            System.out.println("Admin user created successfully with password: admin123");
        }
    }
}