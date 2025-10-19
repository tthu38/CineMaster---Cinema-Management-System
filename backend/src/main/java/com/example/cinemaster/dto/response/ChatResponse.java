package com.example.cinemaster.dto.response;

public class ChatResponse {
    private String answer;

    // Constructors
    public ChatResponse() {}
    public ChatResponse(String answer) { this.answer = answer; }

    // Getter and Setter
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}
