package com.devi.knowledgebase.dto.s3;

public record PresignedUrlResponse(
        String uploadUrl,
        String objectKey
) {
}
