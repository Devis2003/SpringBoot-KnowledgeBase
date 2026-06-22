package com.devi.knowledgebase.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(SqsProperties sqsProperties) {
        return SqsClient.builder()
                .region(Region.of(sqsProperties.region()))
                .build();
    }
}
