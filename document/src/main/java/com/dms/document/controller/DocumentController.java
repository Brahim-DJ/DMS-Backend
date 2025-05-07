package com.dms.document.controller;

import com.dms.document.dto.DocumentRequest;
import com.dms.document.dto.DocumentResponse;
import com.dms.document.dto.FileUploadRequest;
import com.dms.document.service.DocumentService;
import com.dms.document.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final FileService fileService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        return new ResponseEntity<>(documentService.createDocument(request), HttpStatus.CREATED);
    }

    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> createDocumentWithFile(
            @RequestPart("document") @Valid DocumentRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "fileDescription", required = false) String fileDescription) {
        
        // First create the document
        DocumentResponse createdDocument = documentService.createDocument(request);
        
        // Then attach the file to it
        FileUploadRequest fileRequest = FileUploadRequest.builder()
                .documentId(createdDocument.getId())
                .fileDescription(fileDescription)
                .build();
        
        DocumentResponse documentWithFile = fileService.uploadFile(file, fileRequest);
        
        return new ResponseEntity<>(documentWithFile, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocumentsForUser());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponse>> searchDocuments(@RequestParam String keyword) {
        return ResponseEntity.ok(documentService.searchDocumentsByTitle(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id, 
            @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(id, request));
    }

    @PutMapping(value = "/{id}/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> updateDocumentWithFile(
            @PathVariable Long id,
            @RequestPart("document") @Valid DocumentRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "fileDescription", required = false) String fileDescription) {
        
        // First update the document metadata
        DocumentResponse updatedDocument = documentService.updateDocument(id, request);
        
        // Then update/replace the file
        FileUploadRequest fileRequest = FileUploadRequest.builder()
                .documentId(id)
                .fileDescription(fileDescription)
                .build();
        
        DocumentResponse documentWithFile = fileService.uploadFile(file, fileRequest);
        
        return ResponseEntity.ok(documentWithFile);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> getDocumentFileDownloadUrl(@PathVariable Long id) {
        String downloadUrl = fileService.generateDownloadUrl(id);
        return ResponseEntity.ok(downloadUrl);
    }
}