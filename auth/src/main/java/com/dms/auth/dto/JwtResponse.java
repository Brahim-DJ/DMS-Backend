// JwtResponse.java
package com.dms.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String token;
    private Long userId;
    private String email;
    private String role;
}
