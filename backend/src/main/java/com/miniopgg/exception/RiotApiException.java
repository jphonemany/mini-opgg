package com.miniopgg.exception;

public class RiotApiException extends RuntimeException {
    private final int statusCode;

    public RiotApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
