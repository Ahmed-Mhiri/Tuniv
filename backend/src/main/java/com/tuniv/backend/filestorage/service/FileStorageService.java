package com.tuniv.backend.filestorage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;

    public FileStorageService(
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

    public String uploadFile(byte[] fileContent, String originalFilename) throws IOException {
        String fileExtension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFilename.substring(i);
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName)
                .acl("public-read") // Make the file publicly accessible
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));
        
        return String.format("%s/%s/%s", this.endpoint, this.bucketName, uniqueFileName);
    }
}