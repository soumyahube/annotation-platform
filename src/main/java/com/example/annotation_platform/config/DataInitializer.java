package com.example.annotation_platform.config;

import com.example.annotation_platform.entity.Role;
import com.example.annotation_platform.entity.User;
import com.example.annotation_platform.repository.RoleRepository;
import com.example.annotation_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=== INITIALISATION DES DONNÉES ===");

        // Créer les rôles
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
        Role annotatorRole = roleRepository.findByName("ROLE_ANNOTATOR").orElse(null);

        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            adminRole = roleRepository.save(adminRole);
            System.out.println("✅ Rôle ADMIN créé");
        }

        if (annotatorRole == null) {
            annotatorRole = new Role();
            annotatorRole.setName("ROLE_ANNOTATOR");
            annotatorRole = roleRepository.save(annotatorRole);
            System.out.println("✅ Rôle ANNOTATOR créé");
        }

        // Créer admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEnabled(true);
            admin.setRoles(List.of(adminRole));
            userRepository.save(admin);
            System.out.println("✅ Compte admin créé: admin/admin");
        }

        // Créer user1, user2, user3
        for (int i = 1; i <= 3; i++) {
            String username = "user" + i;
            if (userRepository.findByUsername(username).isEmpty()) {
                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(username));
                user.setEnabled(true);
                user.setRoles(List.of(annotatorRole));
                userRepository.save(user);
                System.out.println("✅ Compte " + username + "/" + username + " créé");
            }
        }

        System.out.println("=== FIN INITIALISATION ===");
    }
}