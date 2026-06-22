package com.devi.knowledgebase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(
        String queueUrl,
        String region
) {
}
