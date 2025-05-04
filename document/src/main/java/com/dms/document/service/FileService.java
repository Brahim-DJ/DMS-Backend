package com.dms.document.service;

import com.dms.document.dto.DocumentResponse;
import com.dms.document.dto.FileUploadRequest;
import com.dms.document.exception.ResourceNotFoundException;
import com.dms.document.model.Document;
import com.dms.document.repository.DocumentRepository;
import com.dms.document.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentService documentService;
    
    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 30;

    @Transactional
    public DocumentResponse uploadFile(MultipartFile file, FileUploadRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        // Find document and check permissions
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        if (!userDetails.getDepartmentIds().contains(document.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }
        
        // Delete existing file if present
        if (document.getFileKey() != null) {
            fileStorageService.deleteFile(document.getFileKey());
        }
        
        // Upload new file to S3
        String fileKey = fileStorageService.uploadFile(file, document.getId());
        
        // Update document with file information
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFileSizeBytes(file.getSize());
        document.setFileKey(fileKey);
        document.setFileDescription(request.getFileDescription());
        document.setUpdatedAt(LocalDateTime.now());
        
        Document updatedDocument = documentRepository.save(document);
        
        // Generate a pre-signed URL for the response
        String fileUrl = fileStorageService.generatePresignedUrl(fileKey, PRESIGNED_URL_EXPIRY_MINUTES);
        
        // Map to response
        DocumentResponse response = documentService.mapToDocumentResponse(updatedDocument);
        response.setFileUrl(fileUrl);
        
        return response;
    }

    @Transactional
    public void deleteFile(Long documentId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        if (!userDetails.getDepartmentIds().contains(document.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }
        
        if (document.getFileKey() != null) {
            // Delete from S3
            fileStorageService.deleteFile(document.getFileKey());
            
            // Update document to remove file references
            document.setFileKey(null);
            document.setFileName(null);
            document.setFileType(null);
            document.setFileSizeBytes(null);
            document.setFileDescription(null);
            document.setUpdatedAt(LocalDateTime.now());
            
            documentRepository.save(document);
            log.info("File deleted for document ID: {}", documentId);
        } else {
            log.warn("No file found to delete for document ID: {}", documentId);
        }
    }

    public String generateDownloadUrl(Long documentId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        if (!userDetails.getDepartmentIds().contains(document.getDepartmentId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }
        
        if (document.getFileKey() == null) {
            throw new ResourceNotFoundException("Document has no file attached");
        }
        
        return fileStorageService.generatePresignedUrl(document.getFileKey(), PRESIGNED_URL_EXPIRY_MINUTES);
    }
}