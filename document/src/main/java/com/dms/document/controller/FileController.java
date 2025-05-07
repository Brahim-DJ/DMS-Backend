package com.dms.document.controller;

import com.dms.document.dto.DocumentResponse;
import com.dms.document.dto.FileUploadRequest;
import com.dms.document.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentId") Long documentId,
            @RequestParam(value = "fileDescription", required = false) String fileDescription) {
        
        FileUploadRequest request = FileUploadRequest.builder()
                .documentId(documentId)
                .fileDescription(fileDescription)
                .build();
        
        return ResponseEntity.ok(fileService.uploadFile(file, request));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable Long documentId) {
        fileService.deleteFile(documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> getFileDownloadUrl(@PathVariable Long documentId) {
        String downloadUrl = fileService.generateDownloadUrl(documentId);
        return ResponseEntity.ok(downloadUrl);
    }
}