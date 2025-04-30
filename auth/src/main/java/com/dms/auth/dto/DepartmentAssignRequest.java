// DepartmentAssignRequest.java
package com.dms.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class DepartmentAssignRequest {
    @NotEmpty(message = "Department IDs cannot be empty")
    private Set<Long> departmentIds;
}
