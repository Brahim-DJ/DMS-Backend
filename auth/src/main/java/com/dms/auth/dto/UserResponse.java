// UserResponse.java
package com.dms.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String role;
    private Set<String> departments;
    private String token;
}