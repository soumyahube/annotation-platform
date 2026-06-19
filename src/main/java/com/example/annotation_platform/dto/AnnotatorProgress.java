package com.example.annotation_platform.dto;

public class AnnotatorProgress {
    private Long id;
    private String username;
    private int totalTasks;
    private int completedTasks;
    private int progress;

    // Constructeurs
    public AnnotatorProgress() {}

    public AnnotatorProgress(Long id, String username, int totalTasks, int completedTasks, int progress) {
        this.id = id;
        this.username = username;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.progress = progress;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}