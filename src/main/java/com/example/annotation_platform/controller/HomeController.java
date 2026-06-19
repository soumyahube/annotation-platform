package com.example.annotation_platform.controller;

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
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private AnnotationTaskRepository annotationTaskRepository;

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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home(Model model) {
        User currentUser = getCurrentUser();

        // Vérifier si l'utilisateur est ADMIN
        boolean isAdmin = currentUser != null && currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // Rediriger l'admin vers le dashboard
            return "redirect:/admin/stats";
        } else {
            // Rediriger l'annotateur vers ses tâches
            return "redirect:/annotator/tasks";
        }
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}