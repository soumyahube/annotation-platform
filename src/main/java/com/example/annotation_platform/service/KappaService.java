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

@Service
public class KappaService {

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Calcule le Fleiss Kappa pour un dataset
     */
    public double calculateFleissKappa(Dataset dataset) {
        List<TextUnit> textUnits = dataset.getTextUnits();
        List<User> annotators = userRepository.findByRolesName("ROLE_ANNOTATOR");

        if (textUnits.isEmpty() || annotators.size() < 2) {
            return 0.0;
        }

        // Récupérer toutes les classes possibles
        String[] classes = dataset.getClasses().split(";");
        int n = annotators.size();      // Nombre d'annotateurs
        int k = classes.length;         // Nombre de classes
        int N = textUnits.size();       // Nombre d'items (textes)

        // Matrice des annotations : [item][categorie] = nombre d'annotateurs
        int[][] ratings = new int[N][k];

        // Remplir la matrice
        for (int i = 0; i < textUnits.size(); i++) {
            TextUnit textUnit = textUnits.get(i);
            List<Annotation> annotations = annotationRepository.findByTextUnit(textUnit);

            for (Annotation annotation : annotations) {
                for (int c = 0; c < k; c++) {
                    if (annotation.getSelectedClass().equals(classes[c])) {
                        ratings[i][c]++;
                        break;
                    }
                }
            }
        }

        // Calculer p_j (proportion de toutes les annotations assignées à la classe j)
        double[] p_j = new double[k];
        int totalAssignments = N * n;

        for (int j = 0; j < k; j++) {
            int sum = 0;
            for (int i = 0; i < N; i++) {
                sum += ratings[i][j];
            }
            p_j[j] = (double) sum / totalAssignments;
        }

        // Calculer P_i (accord pour chaque item)
        double[] P_i = new double[N];
        for (int i = 0; i < N; i++) {
            double sum = 0;
            for (int j = 0; j < k; j++) {
                sum += Math.pow(ratings[i][j], 2);
            }
            P_i[i] = (sum - n) / (n * (n - 1));
        }

        // Calculer P_bar (moyenne des P_i)
        double P_bar = 0;
        for (int i = 0; i < N; i++) {
            P_bar += P_i[i];
        }
        P_bar /= N;

        // Calculer P_e (accord attendu par hasard)
        double P_e = 0;
        for (int j = 0; j < k; j++) {
            P_e += Math.pow(p_j[j], 2);
        }

        // Calculer Kappa
        if (P_e == 1.0) {
            return 1.0;
        }

        double kappa = (P_bar - P_e) / (1 - P_e);

        return Math.round(kappa * 100.0) / 100.0;
    }

    /**
     * Interprétation du Kappa
     */
    public String interpretKappa(double kappa) {
        if (kappa < 0) return "Pas d'accord ❌";
        if (kappa < 0.21) return "Accord faible ⚠️";
        if (kappa < 0.41) return "Accord correct 👍";
        if (kappa < 0.61) return "Accord modéré ✅";
        if (kappa < 0.81) return "Accord substantiel 🎯";
        return "Accord presque parfait 🏆";
    }

    /**
     * Retourne la classe CSS pour la couleur
     */
    public String getKappaColor(double kappa) {
        if (kappa < 0.21) return "poor";
        if (kappa < 0.41) return "moderate";
        if (kappa < 0.61) return "good";
        return "excellent";
    }
}