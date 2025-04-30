// DepartmentRepository.java
package com.dms.auth.repository;

import com.dms.auth.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Set;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Set<Department> findByIdIn(Set<Long> ids);
}
