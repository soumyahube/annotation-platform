package com.example.annotation_platform.entity;
import lombok.Data;

@Data
public class AnnotatorProgress {
    private Long id;
    private String username;
    private int totalTasks;
    private int completedTasks;
    private int progress;
}