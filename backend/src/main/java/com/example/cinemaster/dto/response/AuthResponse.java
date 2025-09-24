package com.example.cinemaster.dto.response;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {

}