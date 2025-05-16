// UserRepository.java
package com.dms.auth.repository;

import com.dms.auth.model.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "departments")
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
}