package com.dms.document.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Upload a file to MinIO storage
     * @param file The file to upload
     * @param documentId The document ID to associate with the file
     * @return The object key (path) of the uploaded file
     */
    public String uploadFile(MultipartFile file, Long documentId) {
        try {
            // Create unique file key with proper path
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String key = String.format("documents/%d/%s%s", 
                    documentId, 
                    UUID.randomUUID().toString(),
                    fileExtension);

            ensureBucketExists();

            // Determine correct content type based on file extension
            String contentType = determineContentType(file.getOriginalFilename(), file.getContentType());
            
            // Upload file to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .contentType(contentType)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build()
            );

            log.info("File uploaded successfully to path: {}", key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Delete a file from MinIO storage
     * @param fileKey The object key of the file to delete
     */
    public void deleteFile(String fileKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build()
            );
            log.info("File deleted successfully: {}", fileKey);
        } catch (Exception e) {
            log.error("Failed to delete file", e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Generate a pre-signed URL for temporary file access
     * @param fileKey The object key of the file
     * @param durationMinutes How long the URL should be valid for (in minutes)
     * @return A pre-signed URL for file access
     */
    public String generatePresignedUrl(String fileKey, int durationMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .method(Method.GET)
                    .expiry(durationMinutes, TimeUnit.MINUTES)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            if (!bucketExists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                log.info("Bucket created: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to check/create bucket", e);
            throw new RuntimeException("Failed to check/create bucket: " + e.getMessage());
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Determines the correct content type based on file extension and fallback content type
     * @param filename The original filename with extension
     * @param fallbackContentType The content type provided by the client (may be incorrect)
     * @return The correct content type
     */
    private String determineContentType(String filename, String fallbackContentType) {
        if (filename == null || filename.isEmpty()) {
            return fallbackContentType;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        
        // Map common file extensions to their correct MIME types
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".pdf":
                return "application/pdf";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".txt":
                return "text/plain";
            default:
                // If we can't determine from extension, use the provided content type
                return fallbackContentType;
        }
    }
}