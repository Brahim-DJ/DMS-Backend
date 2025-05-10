package com.dms.auth.util;

import com.dms.auth.model.Department;
import com.dms.auth.security.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Set<Department> departments = new HashSet<>();
        if (annotation.departmentIds().length > 0) {
            for (long id : annotation.departmentIds()) {
                departments.add(new Department(id, "Department_" + id));
            }
        }

        UserDetailsImpl principal = UserDetailsImpl.builder()
                .id(annotation.id())
                .email(annotation.username())
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + annotation.role())))
                .departments(departments)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, "password", principal.getAuthorities());
        context.setAuthentication(auth);
        return context;
    }
}