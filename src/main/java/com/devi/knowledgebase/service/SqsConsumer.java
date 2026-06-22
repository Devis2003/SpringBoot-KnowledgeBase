package com.devi.knowledgebase.service;

import com.devi.knowledgebase.config.SqsProperties;
import com.devi.knowledgebase.dto.event.ArticleEvent;
import com.devi.knowledgebase.entity.Article;
import com.devi.knowledgebase.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqsConsumer {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SqsConsumer(
            SqsClient sqsClient,
            SqsProperties sqsProperties,
            ArticleRepository articleRepository
    ) {
        this.sqsClient = sqsClient;
        this.sqsProperties = sqsProperties;
        this.articleRepository = articleRepository;
    }

    @Scheduled(fixedDelay = 10000)
    public void pollMessages() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.queueUrl())
                .maxNumberOfMessages(5)
                .waitTimeSeconds(5)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        for (Message message : messages) {
            try {
                ArticleEvent event = objectMapper.readValue(message.body(), ArticleEvent.class);

                System.out.println("Received SQS event: " + event);

                processEvent(event);

                deleteMessage(message);

            } catch (Exception e) {
                System.out.println("Failed to process SQS message: " + e.getMessage());
            }
        }
    }

    private void processEvent(ArticleEvent event) {
        if ("ARTICLE_CREATED".equals(event.eventType()) || "ARTICLE_UPDATED".equals(event.eventType())) {
            processArticlePostProcessing(event.articleId());
        } else if ("ARTICLE_VIEWED".equals(event.eventType())) {
            incrementViewCount(event.articleId());
        }
    }

    private void incrementViewCount(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found for view count update"));

        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);

        System.out.println("Updated view count for article: " + articleId);
    }

    private void processArticlePostProcessing(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found for post-processing"));

        String content = article.getContent();

        List<String> links = extractLinks(content);
        String summary = generateSummary(content);

        System.out.println("Post-processing article ID: " + articleId);
        System.out.println("Generated summary: " + summary);
        System.out.println("Extracted links: " + links);
    }

    private List<String> extractLinks(String content) {
        Matcher matcher = URL_PATTERN.matcher(content == null ? "" : content);

        return matcher.results()
                .map(result -> result.group())
                .toList();
    }

    private String generateSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String plainText = content
                .replaceAll("#", "")
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", "")
                .replaceAll("\\[[^]]*]\\([^)]*\\)", "")
                .replaceAll("\\s+", " ")
                .trim();

        return plainText.length() <= 120
                ? plainText
                : plainText.substring(0, 120) + "...";
    }

    private void deleteMessage(Message message) {
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(sqsProperties.queueUrl())
                .receiptHandle(message.receiptHandle())
                .build();

        sqsClient.deleteMessage(deleteRequest);

        System.out.println("Deleted SQS message after processing");
    }
}