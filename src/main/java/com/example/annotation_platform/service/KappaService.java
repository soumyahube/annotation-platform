package com.example.annotation_platform.service;

import com.example.annotation_platform.entity.Annotation;
import com.example.annotation_platform.entity.Dataset;
import com.example.annotation_platform.entity.TextUnit;
import com.example.annotation_platform.entity.User;
import com.example.annotation_platform.repository.AnnotationRepository;
import com.example.annotation_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KappaService {

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private UserRepository userRepository;

    public double calculateFleissKappa(Dataset dataset) {
        List<TextUnit> textUnits = dataset.getTextUnits();

        System.out.println("=== CALCUL KAPPA ===");
        System.out.println("Dataset ID: " + dataset.getId());
        System.out.println("Dataset Name: " + dataset.getName());
        System.out.println("Nombre de textes total : " + textUnits.size());

        if (textUnits.isEmpty()) {
            return 0.0;
        }

        // 🔧 CORRECTION : Ne prendre que les textes avec AU MOINS 2 annotations
        List<TextUnit> annotatedTexts = new ArrayList<>();
        Map<Long, List<Annotation>> annotationsByText = new HashMap<>();

        for (TextUnit textUnit : textUnits) {
            List<Annotation> annotations = annotationRepository.findByTextUnit(textUnit);

            // 🔧 IGNORER les textes avec moins de 2 annotations
            if (annotations.size() >= 2) {
                annotatedTexts.add(textUnit);
                annotationsByText.put(textUnit.getId(), annotations);
                System.out.println("Texte " + textUnit.getId() + " : " + annotations.size() + " annotations (inclus)");
                for (Annotation ann : annotations) {
                    System.out.println("   - annotateur " + ann.getAnnotator().getId() + " : " + ann.getSelectedClass());
                }
            } else if (annotations.size() == 1) {
                System.out.println("Texte " + textUnit.getId() + " : " + annotations.size() + " annotation (IGNORE - pas assez de données)");
            }
        }

        if (annotatedTexts.isEmpty()) {
            System.out.println("Aucun texte avec assez d'annotations (minimum 2)");
            return 0.0;
        }

        System.out.println("Textes inclus dans le calcul : " + annotatedTexts.size() + " / " + textUnits.size());

        // Récupérer les annotateurs UNIQUEMENT pour ce dataset
        List<User> annotators = annotationsByText.values().stream()
                .flatMap(List::stream)
                .map(Annotation::getAnnotator)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("Nombre d'annotateurs : " + annotators.size());

        if (annotators.size() < 2) {
            System.out.println("Moins de 2 annotateurs");
            return 0.0;
        }

        String[] classes = dataset.getClasses().split(";");
        int n = annotators.size();
        int k = classes.length;
        int N = annotatedTexts.size();

        System.out.println("Nombre de classes : " + k);
        System.out.println("Classes : " + Arrays.toString(classes));

        int[][] ratings = new int[N][k];

        for (int i = 0; i < annotatedTexts.size(); i++) {
            TextUnit textUnit = annotatedTexts.get(i);
            List<Annotation> annotations = annotationsByText.get(textUnit.getId());

            for (Annotation annotation : annotations) {
                if (!annotators.contains(annotation.getAnnotator())) {
                    continue;
                }
                for (int c = 0; c < k; c++) {
                    if (annotation.getSelectedClass().trim().equals(classes[c].trim())) {
                        ratings[i][c]++;
                        break;
                    }
                }
            }
        }

        // Afficher la matrice
        System.out.println("\nMatrice des annotations (textes avec 2+ annotations) :");
        for (int i = 0; i < N; i++) {
            System.out.print("Texte " + (i+1) + " : ");
            for (int j = 0; j < k; j++) {
                System.out.print(ratings[i][j] + " ");
            }
            System.out.println();
        }

        int totalAssignments = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < k; j++) {
                totalAssignments += ratings[i][j];
            }
        }

        System.out.println("Total assignments : " + totalAssignments);

        if (totalAssignments == 0) {
            return 0.0;
        }

        // Calculer p_j
        double[] p_j = new double[k];
        for (int j = 0; j < k; j++) {
            int sum = 0;
            for (int i = 0; i < N; i++) {
                sum += ratings[i][j];
            }
            p_j[j] = (double) sum / totalAssignments;
            System.out.println("p_" + j + " = " + p_j[j]);
        }

        // Calculer P_i
        double[] P_i = new double[N];
        for (int i = 0; i < N; i++) {
            double sum = 0;
            for (int j = 0; j < k; j++) {
                sum += Math.pow(ratings[i][j], 2);
            }
            P_i[i] = (sum - n) / (n * (n - 1));
            System.out.println("P_" + i + " = " + P_i[i]);
        }

        // Calculer P_bar
        double P_bar = 0;
        for (int i = 0; i < N; i++) {
            P_bar += P_i[i];
        }
        P_bar /= N;
        System.out.println("P_bar = " + P_bar);

        // Calculer P_e
        double P_e = 0;
        for (int j = 0; j < k; j++) {
            P_e += Math.pow(p_j[j], 2);
        }
        System.out.println("P_e = " + P_e);

        if (P_e == 1.0) {
            return 1.0;
        }

        double kappa = (P_bar - P_e) / (1 - P_e);
        System.out.println("Kappa brut = " + kappa);

        // Si Kappa est négatif, le mettre à 0
        if (kappa < 0) {
            kappa = 0.0;
        }

        double result = Math.round(kappa * 100.0) / 100.0;
        System.out.println("Kappa final = " + result);
        System.out.println("=== FIN CALCUL KAPPA ===");

        return result;
    }

    public String interpretKappa(double kappa) {
        if (kappa < 0.01) return "Pas d'accord";
        if (kappa < 0.21) return "Accord faible";
        if (kappa < 0.41) return "Accord correct";
        if (kappa < 0.61) return "Accord modere";
        if (kappa < 0.81) return "Accord substantiel";
        return "Accord presque parfait";
    }

    public String getKappaColor(double kappa) {
        if (kappa < 0.21) return "poor";
        if (kappa < 0.41) return "moderate";
        if (kappa < 0.61) return "good";
        return "excellent";
    }
}