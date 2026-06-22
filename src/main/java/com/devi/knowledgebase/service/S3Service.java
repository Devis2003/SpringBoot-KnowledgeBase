package com.devi.knowledgebase.service;

import com.devi.knowledgebase.config.S3Properties;
import com.devi.knowledgebase.dto.s3.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public PresignedUrlResponse generatePresignedUploadUrl(String fileName, String contentType) {
        String objectKey = "articles/images/" + UUID.randomUUID() + "-" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucketName())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Properties.presignedUrlExpirationMinutes()))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        return new PresignedUrlResponse(uploadUrl, objectKey);
    }
}
