package com.example.annotation_platform.service;

import com.example.annotation_platform.dto.SpammerDetectionResult;
import com.example.annotation_platform.entity.Annotation;
import com.example.annotation_platform.entity.AnnotationTask;
import com.example.annotation_platform.entity.TextUnit;
import com.example.annotation_platform.entity.User;
import com.example.annotation_platform.repository.AnnotationRepository;
import com.example.annotation_platform.repository.AnnotationTaskRepository;
import com.example.annotation_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpammerDetectionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private AnnotationTaskRepository annotationTaskRepository;

    /**
     * Détecte les spammeurs parmi tous les annotateurs
     */
    public List<SpammerDetectionResult> detectSpammers() {
        List<User> annotators = userRepository.findByRolesName("ROLE_ANNOTATOR");
        List<SpammerDetectionResult> results = new ArrayList<>();

        for (User annotator : annotators) {
            SpammerDetectionResult result = analyzeAnnotator(annotator);
            if (result != null) {
                results.add(result);
            }
        }

        // Trier par score de spam (le plus suspect en premier)
        results.sort((a, b) -> Integer.compare(b.getSpamScore(), a.getSpamScore()));

        return results;
    }

    /**
     * Analyse un annotateur spécifique
     */
    private SpammerDetectionResult analyzeAnnotator(User annotator) {
        List<Annotation> annotations = annotationRepository.findByAnnotator(annotator);

        if (annotations.isEmpty()) {
            SpammerDetectionResult result = new SpammerDetectionResult();
            result.setAnnotatorId(annotator.getId());
            result.setUsername(annotator.getUsername());
            result.setSpamScore(0);
            result.setSuspicious(false);
            result.setRecommendation("Aucune annotation effectuée pour le moment.");
            return result;
        }

        SpammerDetectionResult result = new SpammerDetectionResult();
        result.setAnnotatorId(annotator.getId());
        result.setUsername(annotator.getUsername());

        // 1. Temps moyen d'annotation
        double avgTime = calculateAverageAnnotationTime(annotations);
        result.setAvgTimePerAnnotation(avgTime);
        result.setTooFast(avgTime < 5.0); // Moins de 5 secondes

        // 2. Distribution des classes
        Map<String, Long> classDistribution = calculateClassDistribution(annotations);
        result.setClassDistribution(classDistribution);

        // 3. Taux de désaccord avec la majorité
        double disagreementRate = calculateDisagreementRate(annotator, annotations);
        result.setDisagreementRate(disagreementRate);

        // 4. Score de spam (0-100)
        int spamScore = calculateSpamScore(avgTime, classDistribution, disagreementRate, annotations.size());
        result.setSpamScore(spamScore);
        result.setSuspicious(spamScore > 60);

        // 5. Recommandation
        result.setRecommendation(generateRecommendation(result));

        return result;
    }

    /**
     * Calcule le temps moyen d'annotation (en secondes)
     */
    private double calculateAverageAnnotationTime(List<Annotation> annotations) {
        if (annotations.size() < 2) {
            return 10.0; // Valeur par défaut
        }

        List<Annotation> sorted = new ArrayList<>(annotations);
        sorted.sort(Comparator.comparing(Annotation::getCreatedAt));

        long totalSeconds = 0;
        int count = 0;

        for (int i = 1; i < sorted.size(); i++) {
            LocalDateTime prev = sorted.get(i-1).getCreatedAt();
            LocalDateTime curr = sorted.get(i).getCreatedAt();
            long seconds = ChronoUnit.SECONDS.between(prev, curr);
            if (seconds > 0 && seconds < 300) { // Ignorer les écarts trop grands
                totalSeconds += seconds;
                count++;
            }
        }

        return count > 0 ? (double) totalSeconds / count : 10.0;
    }

    /**
     * Calcule la distribution des classes choisies
     */
    private Map<String, Long> calculateClassDistribution(List<Annotation> annotations) {
        return annotations.stream()
                .collect(Collectors.groupingBy(Annotation::getSelectedClass, Collectors.counting()));
    }

    /**
     * Calcule le taux de désaccord avec la majorité des annotateurs
     */
    private double calculateDisagreementRate(User annotator, List<Annotation> annotations) {
        if (annotations.isEmpty()) {
            return 0;
        }

        int disagreements = 0;
        int total = 0;

        for (Annotation annotation : annotations) {
            TextUnit textUnit = annotation.getTextUnit();

            // Récupérer toutes les annotations de ce texte
            List<Annotation> textAnnotations = annotationRepository.findByTextUnit(textUnit);

            if (textAnnotations.size() > 1) {
                // Trouver la classe majoritaire
                Map<String, Long> voteCount = textAnnotations.stream()
                        .collect(Collectors.groupingBy(Annotation::getSelectedClass, Collectors.counting()));

                String majorityClass = voteCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

                // Vérifier si l'annotateur est en désaccord
                if (majorityClass != null && !annotation.getSelectedClass().equals(majorityClass)) {
                    disagreements++;
                }
                total++;
            }
        }

        return total > 0 ? (disagreements * 100.0 / total) : 0;
    }

    /**
     * Calcule le score de spam (0-100)
     */
    private int calculateSpamScore(double avgTime, Map<String, Long> classDistribution, double disagreementRate, int annotationCount) {
        int score = 0;

        // Critère 1 : Temps trop rapide (max 30 points)
        if (avgTime < 3) score += 30;
        else if (avgTime < 5) score += 20;
        else if (avgTime < 8) score += 10;
        else if (avgTime > 30) score += 5; // Trop lent aussi suspect

        // Critère 2 : Désaccord avec la majorité (max 40 points)
        if (disagreementRate > 70) score += 40;
        else if (disagreementRate > 50) score += 25;
        else if (disagreementRate > 30) score += 15;

        // Critère 3 : Distribution déséquilibrée (max 30 points)
        if (!classDistribution.isEmpty()) {
            long total = classDistribution.values().stream().mapToLong(Long::longValue).sum();
            long maxClass = classDistribution.values().stream().max(Long::compareTo).orElse(0L);
            double maxPercentage = (maxClass * 100.0) / total;

            if (maxPercentage > 90) score += 30;
            else if (maxPercentage > 75) score += 20;
            else if (maxPercentage > 60) score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * Génère une recommandation basée sur l'analyse
     */
    private String generateRecommendation(SpammerDetectionResult result) {
        if (!result.isSuspicious()) {
            return "✅ Comportement normal. Aucune action requise.";
        }

        List<String> issues = new ArrayList<>();

        if (result.isTooFast()) {
            issues.add("annotation trop rapide (" + String.format("%.1f", result.getAvgTimePerAnnotation()) + "s en moyenne)");
        }

        if (result.getDisagreementRate() > 50) {
            issues.add("taux de désaccord élevé (" + String.format("%.1f", result.getDisagreementRate()) + "%)");
        }

        // Vérifier la distribution des classes
        if (result.getClassDistribution() != null && !result.getClassDistribution().isEmpty()) {
            long total = result.getClassDistribution().values().stream().mapToLong(Long::longValue).sum();
            long maxClass = result.getClassDistribution().values().stream().max(Long::compareTo).orElse(0L);
            double maxPercentage = (maxClass * 100.0) / total;
            if (maxPercentage > 80) {
                String dominantClass = result.getClassDistribution().entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("inconnue");
                issues.add("choix trop fréquent de la classe '" + dominantClass + "' (" + String.format("%.0f", maxPercentage) + "%)");
            }
        }

        if (issues.isEmpty()) {
            return "⚠️ Comportement suspect à surveiller.";
        }

        return "⚠️ ALERTE : " + String.join(", ", issues) + ". À surveiller.";
    }

    /**
     * Détecte spécifiquement les annotateurs qui annotent aléatoirement
     */
    public List<User> detectRandomAnnotators() {
        List<User> annotators = userRepository.findByRolesName("ROLE_ANNOTATOR");
        List<User> randomAnnotators = new ArrayList<>();

        for (User annotator : annotators) {
            List<Annotation> annotations = annotationRepository.findByAnnotator(annotator);
            if (isAnnotatingRandomly(annotations)) {
                randomAnnotators.add(annotator);
            }
        }

        return randomAnnotators;
    }

    private boolean isAnnotatingRandomly(List<Annotation> annotations) {
        if (annotations.size() < 10) return false;

        Map<String, Long> distribution = calculateClassDistribution(annotations);
        double disagreementRate = calculateDisagreementRate(annotations.get(0).getAnnotator(), annotations);

        // Distribution uniforme + fort désaccord = annotation aléatoire
        long total = distribution.values().stream().mapToLong(Long::longValue).sum();
        double expectedPerClass = (double) total / distribution.size();
        boolean uniformDistribution = distribution.values().stream()
                .allMatch(count -> Math.abs(count - expectedPerClass) < expectedPerClass * 0.3);

        return uniformDistribution && disagreementRate > 60;
    }
}