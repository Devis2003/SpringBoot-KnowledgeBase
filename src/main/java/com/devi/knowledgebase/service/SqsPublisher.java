package com.devi.knowledgebase.service;

import com.devi.knowledgebase.config.SqsProperties;
import com.devi.knowledgebase.dto.event.ArticleEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
public class SqsPublisher {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishArticleEvent(ArticleEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsProperties.queueUrl())
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(request);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize article event", e);
        }
    }
}
