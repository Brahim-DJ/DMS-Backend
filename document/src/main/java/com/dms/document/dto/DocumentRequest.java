package com.dms.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotNull(message = "Department ID is required")
    private Long departmentId;
    
    @NotNull(message = "Document category is required")
    private Long categoryId;
    
    private String fileName;
    private String fileDescription;
    private String fileType;
    private Long fileSizeBytes;
}