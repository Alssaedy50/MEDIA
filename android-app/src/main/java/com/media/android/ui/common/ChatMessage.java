package com.media.android.ui.common;

import java.util.UUID;

/** A single message in the MEDIA chat transcript. */
public final class ChatMessage {

    public enum Role { USER, MEDIA }

    public final String id;
    public final Role role;
    public final String content;
    public final double confidence;
    public final String evidenceSummary;
    public final long timestamp;
    /** True while a MEDIA answer is still being produced (drives the typing indicator). */
    public boolean pending;

    public ChatMessage(String id, Role role, String content, double confidence,
                       String evidenceSummary, long timestamp) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.role = role;
        this.content = content == null ? "" : content;
        this.confidence = confidence;
        this.evidenceSummary = evidenceSummary == null ? "" : evidenceSummary;
        this.timestamp = timestamp;
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(null, Role.USER, text, 0, "", System.currentTimeMillis());
    }

    public static ChatMessage media(String content, double confidence, String evidence) {
        return new ChatMessage(null, Role.MEDIA, content, confidence, evidence,
                System.currentTimeMillis());
    }

    public boolean isUser() {
        return role == Role.USER;
    }
}
