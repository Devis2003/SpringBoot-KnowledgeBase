package com.devi.knowledgebase.exception;

public class UnauthorizedArticleAccessException extends RuntimeException {

    public UnauthorizedArticleAccessException(String message) {
        super(message);
    }
}