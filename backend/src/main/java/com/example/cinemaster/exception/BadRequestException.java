// src/main/java/com/example/cinemaster/exception/BadRequestException.java
package com.example.cinemaster.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
