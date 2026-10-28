package com.example.myapp.domain.model;

/**
 * Model chứa thông tin cảnh báo.
 */
public class AlertMessage {
    public enum Priority {
        DANGER,    // Nguy hiểm (Đỏ)
        WARNING,   // Nhắc nhở (Vàng)
        INFO       // Thông báo (Xanh/Trắng)
    }

    private String title;
    private String content;
    private Priority priority;
    private String timestamp;

    public AlertMessage(String title, String content, Priority priority, String timestamp) {
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    // Getters
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Priority getPriority() { return priority; }
    public String getTimestamp() { return timestamp; }
}