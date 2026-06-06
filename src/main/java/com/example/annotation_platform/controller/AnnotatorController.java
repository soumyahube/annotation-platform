package com.example.annotation_platform.controller;

import com.example.annotation_platform.entity.Annotation;
import com.example.annotation_platform.entity.AnnotationTask;
import com.example.annotation_platform.entity.Dataset;
import com.example.annotation_platform.entity.TextUnit;
import com.example.annotation_platform.entity.User;
import com.example.annotation_platform.repository.AnnotationRepository;
import com.example.annotation_platform.repository.AnnotationTaskRepository;
import com.example.annotation_platform.repository.DatasetRepository;
import com.example.annotation_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/annotator")
public class AnnotatorController {

    @Autowired
    private AnnotationTaskRepository annotationTaskRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    /**
     * Récupère l'utilisateur connecté
     */
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Page d'accueil de l'annotateur - Liste des tâches
     */
    @GetMapping("/tasks")
    public String tasks(Model model) {
        User currentUser = getCurrentUser();

        // Récupérer toutes les tâches non complétées de l'annotateur
        List<AnnotationTask> pendingTasks = annotationTaskRepository
                .findByAnnotatorAndCompleted(currentUser, false);

        // Récupérer l'historique des tâches complétées
        List<AnnotationTask> completedTasks = annotationTaskRepository
                .findByAnnotatorAndCompleted(currentUser, true);

        // Statistiques
        int totalTasks = pendingTasks.size() + completedTasks.size();
        int completedCount = completedTasks.size();
        int progress = totalTasks > 0 ? (completedCount * 100 / totalTasks) : 0;

        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("progress", progress);

        return "annotator/tasks";
    }

    /**
     * Page d'annotation d'un texte spécifique
     */
    @GetMapping("/annotate/{taskId}")
    public String annotateForm(@PathVariable Long taskId, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        AnnotationTask task = annotationTaskRepository.findById(taskId).orElse(null);

        // Vérifier que la tâche appartient bien à l'utilisateur connecté
        if (task == null || !task.getAnnotator().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "❌ Tâche non trouvée ou non autorisée !");
            return "redirect:/annotator/tasks";
        }

        // Vérifier si déjà complétée
        if (task.isCompleted()) {
            redirectAttributes.addFlashAttribute("error", "❌ Cette tâche a déjà été complétée !");
            return "redirect:/annotator/tasks";
        }

        Dataset dataset = task.getDataset();
        TextUnit textUnit = task.getTextUnit();

        // Récupérer les classes possibles
        String[] classes = dataset.getClasses().split(";");

        model.addAttribute("task", task);
        model.addAttribute("dataset", dataset);
        model.addAttribute("textUnit", textUnit);
        model.addAttribute("classes", classes);

        return "annotator/annotate";
    }

    /**
     * Sauvegarde de l'annotation
     */
    @PostMapping("/annotate/{taskId}")
    public String saveAnnotation(@PathVariable Long taskId,
                                 @RequestParam String selectedClass,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        AnnotationTask task = annotationTaskRepository.findById(taskId).orElse(null);

        if (task == null || !task.getAnnotator().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "❌ Tâche non autorisée !");
            return "redirect:/annotator/tasks";
        }

        if (task.isCompleted()) {
            redirectAttributes.addFlashAttribute("error", "❌ Tâche déjà complétée !");
            return "redirect:/annotator/tasks";
        }

        // Créer l'annotation
        Annotation annotation = new Annotation();
        annotation.setTextUnit(task.getTextUnit());
        annotation.setAnnotator(currentUser);
        annotation.setSelectedClass(selectedClass);
        annotation.setCreatedAt(LocalDateTime.now());
        annotationRepository.save(annotation);

        // Marquer la tâche comme complétée
        task.setCompleted(true);
        annotationTaskRepository.save(task);

        redirectAttributes.addFlashAttribute("success", "✅ Annotation sauvegardée avec succès !");

        return "redirect:/annotator/tasks";
    }
    // Statistiques personnelles de l'annotateur
    @GetMapping("/stats")
    public String stats(Model model) {
        User currentUser = getCurrentUser();

        // Statistiques des tâches
        long totalTasks = annotationTaskRepository.countByAnnotator(currentUser);
        long completedTasks = annotationTaskRepository.countByAnnotatorAndCompleted(currentUser, true);
        long pendingTasks = totalTasks - completedTasks;
        int progress = totalTasks > 0 ? (int) (completedTasks * 100 / totalTasks) : 0;

        // Récupérer toutes les annotations de l'utilisateur
        List<Annotation> annotations = annotationRepository.findByAnnotator(currentUser);
        long totalAnnotations = annotations.size();

        // Distribution des classes
        Map<String, Long> classDistribution = new HashMap<>();
        for (Annotation annotation : annotations) {
            String className = annotation.getSelectedClass();
            classDistribution.put(className, classDistribution.getOrDefault(className, 0L) + 1);
        }

        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("progress", progress);
        model.addAttribute("totalAnnotations", totalAnnotations);
        model.addAttribute("classDistribution", classDistribution);
        model.addAttribute("annotations", annotations);

        return "annotator/stats";
    }
}