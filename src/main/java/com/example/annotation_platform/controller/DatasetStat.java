package com.example.annotation_platform.controller;

public class DatasetStat {
    private String name;
    private String type;
    private int totalTasks;
    private int completedTasks;
    private int progress;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}