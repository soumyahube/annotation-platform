package com.example.annotation_platform.dto;

import java.util.Map;

public class SpammerDetectionResult {
    private Long annotatorId;
    private String username;
    private double avgTimePerAnnotation;  // Temps moyen en secondes
    private boolean tooFast;              // Moins de 5 secondes
    private Map<String, Long> classDistribution;
    private double disagreementRate;      // Taux de désaccord avec la majorité (%)
    private int spamScore;                // Score global (0-100)
    private boolean isSuspicious;         // Suspect si score > 60
    private String recommendation;        // Recommandation

    // Getters et Setters
    public Long getAnnotatorId() { return annotatorId; }
    public void setAnnotatorId(Long annotatorId) { this.annotatorId = annotatorId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getAvgTimePerAnnotation() { return avgTimePerAnnotation; }
    public void setAvgTimePerAnnotation(double avgTimePerAnnotation) { this.avgTimePerAnnotation = avgTimePerAnnotation; }

    public boolean isTooFast() { return tooFast; }
    public void setTooFast(boolean tooFast) { this.tooFast = tooFast; }

    public Map<String, Long> getClassDistribution() { return classDistribution; }
    public void setClassDistribution(Map<String, Long> classDistribution) { this.classDistribution = classDistribution; }

    public double getDisagreementRate() { return disagreementRate; }
    public void setDisagreementRate(double disagreementRate) { this.disagreementRate = disagreementRate; }

    public int getSpamScore() { return spamScore; }
    public void setSpamScore(int spamScore) { this.spamScore = spamScore; }

    public boolean isSuspicious() { return isSuspicious; }
    public void setSuspicious(boolean suspicious) { isSuspicious = suspicious; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

}