package com.example.smartscan;

public class Note {
    private long id;
    private String userId;
    private String title;
    private String rawText;
    private String summary;
    private String keywords;
    private long createdAt;

    public Note(long id, String userId, String title, String rawText, String summary, String keywords, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.rawText = rawText;
        this.summary = summary;
        this.keywords = keywords;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getRawText() {
        return rawText;
    }

    public String getSummary() {
        return summary;
    }

    public String getKeywords() {
        return keywords;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

