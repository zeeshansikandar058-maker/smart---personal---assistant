package com.spa.notes;

public class Note {
    private final int id;
    private final int userId;
    private final String title;
    private final String content;
    private final String category;
    private final String source; // TEXT or VOICE
    private final String createdAt;

    public Note(int id, int userId, String title, String content, String category, String source, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.source = source;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getSource() { return source; }
    public String getCreatedAt() { return createdAt; }
}
