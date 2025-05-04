package com.dms.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Upload a file to S3 storage
     * @param file The file to upload
     * @param documentId The document ID to associate with the file
     * @return The S3 object key (path) of the uploaded file
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

            // Upload file to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("File uploaded successfully to path: {}", key);
            return key;
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Delete a file from S3 storage
     * @param fileKey The S3 key of the file to delete
     */
    public void deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", fileKey);
        } catch (Exception e) {
            log.error("Failed to delete file", e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Generate a pre-signed URL for temporary file access
     * @param fileKey The S3 key of the file
     * @param durationMinutes How long the URL should be valid for (in minutes)
     * @return A pre-signed URL for file access
     */
    public String generatePresignedUrl(String fileKey, int durationMinutes) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build())
                .build();

        String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.info("Generated presigned URL for file: {}", fileKey);
        return presignedUrl;
    }

    private void ensureBucketExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            // Bucket doesn't exist, create it
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.createBucket(createBucketRequest);
            log.info("Bucket created: {}", bucketName);
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}