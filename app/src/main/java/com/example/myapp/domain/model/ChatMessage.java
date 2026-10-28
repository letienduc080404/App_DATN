package com.example.myapp.domain.model;

/**
 * Model chứa nội dung tin nhắn chat.
 */
public class ChatMessage {
    private String text;
    private boolean isFromUser;

    public ChatMessage(String text, boolean isFromUser) {
        this.text = text;
        this.isFromUser = isFromUser;
    }

    public String getText() {
        return text;
    }

    public boolean isFromUser() {
        return isFromUser;
    }
}