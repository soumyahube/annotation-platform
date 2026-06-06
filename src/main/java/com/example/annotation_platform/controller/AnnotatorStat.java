package com.example.annotation_platform.controller;

public class AnnotatorStat {
    private String username;
    private int totalTasks;
    private int completedTasks;
    private int progress;

    // Constructeurs
    public AnnotatorStat() {}

    public AnnotatorStat(String username, int totalTasks, int completedTasks, int progress) {
        this.username = username;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.progress = progress;
    }

    // Getters et Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}