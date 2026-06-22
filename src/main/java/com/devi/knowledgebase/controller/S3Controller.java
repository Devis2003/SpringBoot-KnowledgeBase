package com.devi.knowledgebase.controller;

import com.devi.knowledgebase.dto.s3.PresignedUrlRequest;
import com.devi.knowledgebase.dto.s3.PresignedUrlResponse;
import com.devi.knowledgebase.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    public PresignedUrlResponse generatePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        return s3Service.generatePresignedUploadUrl(
                request.fileName(),
                request.contentType()
        );
    }
}
