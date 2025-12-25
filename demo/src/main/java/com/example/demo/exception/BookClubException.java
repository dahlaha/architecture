package com.example.demo.exception;

public class BookClubException extends RuntimeException {
    
    public BookClubException(String message) {
        super(message);
    }
    
    public BookClubException(String message, Throwable cause) {
        super(message, cause);
    }
} 