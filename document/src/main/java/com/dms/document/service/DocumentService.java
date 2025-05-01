package com.dms.document.service;

import com.dms.document.dto.DocumentRequest;
import com.dms.document.dto.DocumentResponse;
import com.dms.document.exception.ResourceNotFoundException;
import com.dms.document.model.Document;
import com.dms.document.model.DocumentCategory;
import com.dms.document.repository.DocumentCategoryRepository;
import com.dms.document.repository.DocumentRepository;
import com.dms.document.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentCategoryRepository categoryRepository;

    @Transactional
    public DocumentResponse createDocument(DocumentRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        // Check if user has access to the department
        if (!userDetails.getDepartmentIds().contains(request.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this department");
        }
        
        DocumentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        Document document = Document.builder()
                .title(request.getTitle())
                .departmentId(request.getDepartmentId())
                .category(category)
                .fileName(request.getFileName())
                .fileDescription(request.getFileDescription())
                .fileType(request.getFileType())
                .fileSizeBytes(request.getFileSizeBytes())
                .createdBy(userDetails.getUsername())
                .createdAt(LocalDateTime.now())
                .build();
        
        Document savedDocument = documentRepository.save(document);
        return mapToDocumentResponse(savedDocument);
    }

    public List<DocumentResponse> getAllDocumentsForUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        return documentRepository.findByDepartmentIdIn(userDetails.getDepartmentIds())
                .stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    public DocumentResponse getDocumentById(Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        // Check if user has access to the document's department
        if (!userDetails.getDepartmentIds().contains(document.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }
        
        return mapToDocumentResponse(document);
    }

    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        // Check if user has access to the document's department
        if (!userDetails.getDepartmentIds().contains(document.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }
        
        // Check if user has access to the target department
        if (!userDetails.getDepartmentIds().contains(request.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to the target department");
        }
        
        DocumentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        document.setTitle(request.getTitle());
        document.setDepartmentId(request.getDepartmentId());
        document.setCategory(category);
        document.setFileName(request.getFileName());
        document.setFileDescription(request.getFileDescription());
        document.setFileType(request.getFileType());
        document.setFileSizeBytes(request.getFileSizeBytes());
        document.setUpdatedAt(LocalDateTime.now());
        
        Document updatedDocument = documentRepository.save(document);
        return mapToDocumentResponse(updatedDocument);
    }

    @Transactional
    public void deleteDocument(Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        // Check if user has access to the document's department
        if (!userDetails.getDepartmentIds().contains(document.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }
        
        documentRepository.delete(document);
    }

    public List<DocumentResponse> searchDocumentsByTitle(String keyword) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Set<Long> departmentIds = userDetails.getDepartmentIds();
        
        return documentRepository.searchByTitleInDepartments(keyword, departmentIds)
                .stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }
    
    private DocumentResponse mapToDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .departmentId(document.getDepartmentId())
                .category(new DocumentResponse.CategoryDto(
                    document.getCategory().getId(),
                    document.getCategory().getName()))
                .fileName(document.getFileName())
                .fileDescription(document.getFileDescription())
                .fileType(document.getFileType())
                .fileSizeBytes(document.getFileSizeBytes())
                .createdBy(document.getCreatedBy())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}