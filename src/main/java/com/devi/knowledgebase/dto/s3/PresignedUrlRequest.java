package com.devi.knowledgebase.dto.s3;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String contentType
) {
}
