package com.antigravity.geminiwear;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_MODEL = "model";

    private String role;
    private String text;
    private String base64Image;
    private long timestamp;

    public ChatMessage(String role, String text) {
        this(role, text, null);
    }

    public ChatMessage(String role, String text, String base64Image) {
        this.role = role;
        this.text = text;
        this.base64Image = base64Image;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public boolean hasImage() {
        return base64Image != null && !base64Image.isEmpty();
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}
