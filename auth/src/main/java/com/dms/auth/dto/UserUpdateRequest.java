// UserUpdateRequest.java
package com.dms.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.Set;

@Data
public class UserUpdateRequest {
    @Email(message = "Email should be valid")
    private String email;
    
    private String password;
    
    private String role;
    
    private Set<Long> departmentIds;
}