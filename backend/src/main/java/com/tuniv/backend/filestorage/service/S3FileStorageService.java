package com.tuniv.backend.filestorage.service;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.filestorage.model.FileStorage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorage {

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;

    public S3FileStorageService(
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.endpoint}") String endpoint,
            @Value("${aws.access.key}") String accessKey,
            @Value("${aws.secret.key}") String secretKey
    ) {
        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
    
    // ✨ --- UPDATED: storeFile to match interface --- ✨
    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            return uploadFileToS3(file.getBytes(), file.getOriginalFilename(), subDirectory);
        } catch (IOException e) {
             throw new RuntimeException("Could not read file for upload: " + file.getOriginalFilename(), e);
        }
    }

    private String uploadFileToS3(byte[] fileContent, String originalFilename, String subDirectory) {
        String fileExtension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFilename.substring(i);
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Use subDirectory to create a "folder" in the S3 bucket
        String key = subDirectory + "/" + uniqueFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .acl("public-read") // Make the file publicly accessible
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));

        return String.format("%s/%s/%s", this.endpoint, this.bucketName, key);
    }

    // ✨ --- NEW: DELETE FILE METHOD --- ✨
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // Example URL: https://my-bucket.s3.us-east-1.amazonaws.com/questions/abc.jpg
            // The object key is the part after the bucket name: "questions/abc.jpg"
            String key = fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            System.err.println("Error deleting S3 file: " + fileUrl + " - " + e.getMessage());
        }
    }
}
