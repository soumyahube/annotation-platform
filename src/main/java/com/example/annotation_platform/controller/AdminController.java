package com.example.annotation_platform.controller;

import com.example.annotation_platform.dto.SpammerDetectionResult;
import com.example.annotation_platform.entity.*;
import com.example.annotation_platform.repository.*;
import com.example.annotation_platform.service.KappaService;
import com.example.annotation_platform.service.SpammerDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.example.annotation_platform.controller.AnnotatorProgress;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private TextUnitRepository textUnitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnotationTaskRepository annotationTaskRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @GetMapping("/datasets")
    public String datasets(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Dataset> datasetPage = datasetRepository.findAll(pageable);

        Map<Long, Integer> progressMap = new HashMap<>();
        for (Dataset dataset : datasetPage.getContent()) {
            long totalTasks = annotationTaskRepository.countByDataset(dataset);
            long completedTasks = annotationTaskRepository.countByDatasetAndCompleted(dataset, true);
            int progress = totalTasks > 0 ? (int) (completedTasks * 100 / totalTasks) : 0;
            progressMap.put(dataset.getId(), progress);
        }

        model.addAttribute("datasets", datasetPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", datasetPage.getTotalPages());
        model.addAttribute("totalItems", datasetPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("progressMap", progressMap);

        return "admin/datasets";
    }

    @PostMapping("/datasets/create")
    public String createDataset(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String type,
            @RequestParam String classes,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            Dataset dataset = new Dataset();
            dataset.setName(name);
            dataset.setDescription(description);
            dataset.setType(type);
            dataset.setClasses(classes);
            dataset = datasetRepository.save(dataset);

            List<TextUnit> textUnits = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] columns = line.split(",");
                TextUnit textUnit = new TextUnit();
                textUnit.setDataset(dataset);

                if (type.equals("TEXTE")) {
                    if (columns.length >= 2) {
                        textUnit.setText1(columns[1]);
                    }
                } else {
                    if (columns.length >= 3) {
                        textUnit.setText1(columns[1]);
                        textUnit.setText2(columns[2]);
                    }
                }
                textUnits.add(textUnit);
            }

            textUnitRepository.saveAll(textUnits);

            redirectAttributes.addFlashAttribute("message", "✅ Dataset '" + name + "' importé avec " + textUnits.size() + " textes !");
            redirectAttributes.addFlashAttribute("success", true);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "❌ Erreur: " + e.getMessage());
            redirectAttributes.addFlashAttribute("success", false);
        }

        return "redirect:/admin/datasets";
    }

    @GetMapping("/datasets/{id}/assign")
    public String assignAnnotators(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElse(null);
        List<User> annotators = userRepository.findByRolesName("ROLE_ANNOTATOR");

        model.addAttribute("dataset", dataset);
        model.addAttribute("annotators", annotators);
        return "admin/assign";
    }
    @GetMapping("/datasets/{id}/details")
    public String datasetDetails(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElse(null);

        // Calculer l'avancement global
        int progress = 0;
        List<AnnotatorProgress> annotatorProgressList = new ArrayList<>();

        if (dataset != null) {
            long totalTasks = annotationTaskRepository.countByDataset(dataset);
            long completedTasks = annotationTaskRepository.countByDatasetAndCompleted(dataset, true);
            progress = totalTasks > 0 ? (int) (completedTasks * 100 / totalTasks) : 0;

            // Récupérer tous les annotateurs
            List<User> allAnnotators = userRepository.findAll();
            for (User annotator : allAnnotators) {
                // Vérifier si l'annotateur a des tâches pour ce dataset
                long total = annotationTaskRepository.countByAnnotatorAndDataset(annotator, dataset);
                if (total > 0) {
                    long completed = annotationTaskRepository.countByAnnotatorAndDatasetAndCompleted(annotator, dataset, true);
                    int annotatorProgress = (int) (completed * 100 / total);

                    AnnotatorProgress ap = new AnnotatorProgress();
                    ap.setId(annotator.getId());
                    ap.setUsername(annotator.getUsername());
                    ap.setTotalTasks((int) total);
                    ap.setCompletedTasks((int) completed);
                    ap.setProgress(annotatorProgress);
                    annotatorProgressList.add(ap);
                }
            }
        }

        model.addAttribute("dataset", dataset);
        model.addAttribute("progress", progress);
        model.addAttribute("annotators", annotatorProgressList);

        return "admin/dataset-details";
    }
    @PostMapping("/datasets/{id}/assign/submit")
    public String submitAssign(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> annotatorIds,
            RedirectAttributes redirectAttributes) {

        if (annotatorIds == null || annotatorIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "❌ Vous devez sélectionner au moins un annotateur !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/datasets/" + id + "/assign";
        }

        // Vérification : Moins de 3 annotateurs = avertissement (pas blocage)
        if (annotatorIds.size() < 3) {
            redirectAttributes.addFlashAttribute("message",
                    "⚠️ ATTENTION : Vous n'avez sélectionné que " + annotatorIds.size() +
                            " annotateur(s). L'assignation a été effectuée mais le minimum recommandé est 3 annotateurs par texte.");
            redirectAttributes.addFlashAttribute("success", false);
        }

        Dataset dataset = datasetRepository.findById(id).orElse(null);
        if (dataset == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Dataset non trouvé !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/datasets";
        }

        List<User> annotators = userRepository.findAllById(annotatorIds);
        List<TextUnit> textUnits = dataset.getTextUnits();

        int totalTasks = 0;
        int existingTasks = 0;

        for (TextUnit textUnit : textUnits) {
            for (User annotator : annotators) {
                boolean exists = annotationTaskRepository.findByTextUnitAndAnnotator(textUnit, annotator).isPresent();

                if (!exists) {
                    AnnotationTask task = new AnnotationTask(textUnit, annotator, dataset);
                    annotationTaskRepository.save(task);
                    totalTasks++;
                } else {
                    existingTasks++;
                }
            }
        }

        redirectAttributes.addFlashAttribute("message",
                "✅ " + totalTasks + " nouvelles tâches créées pour " + annotators.size() + " annotateurs." +
                        (existingTasks > 0 ? " (" + existingTasks + " tâches existaient déjà)" : ""));
        redirectAttributes.addFlashAttribute("success", true);

        return "redirect:/admin/datasets";
    }
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    // Afficher la page de gestion des annotateurs
    @GetMapping("/users")
    public String users(Model model) {
        List<User> annotators = userRepository.findByRolesName("ROLE_ANNOTATOR");
        model.addAttribute("annotators", annotators);
        model.addAttribute("generatedPassword", "");
        return "admin/users";
    }
    /**
     * Génère un mot de passe aléatoire de 8 caractères
     */
    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }
        return password.toString();
    }
    // Ajouter un annotateur
    @PostMapping("/users/add")
    public String addUser(@RequestParam String username,
                          @RequestParam(required = false) String firstName,
                          @RequestParam(required = false) String lastName,
                          @RequestParam(required = false) String email,
                          RedirectAttributes redirectAttributes) {

        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("message", "❌ Le nom d'utilisateur '" + username + "' existe déjà !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/users";
        }

        // Générer un mot de passe aléatoire
        String generatedPassword = generateRandomPassword();

        Role annotatorRole = roleRepository.findByName("ROLE_ANNOTATOR")
                .orElseThrow(() -> new RuntimeException("Rôle ANNOTATOR non trouvé"));

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(generatedPassword));
        newUser.setEnabled(true);
        newUser.setRoles(List.of(annotatorRole));

        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("message",
                "✅ Annotateur '" + username + "' créé avec succès !<br>" +
                        "📧 Identifiants :<br>" +
                        "• Nom d'utilisateur : <strong>" + username + "</strong><br>" +
                        "• Mot de passe : <strong>" + generatedPassword + "</strong>");
        redirectAttributes.addFlashAttribute("success", true);

        return "redirect:/admin/users";
    }



    // Désactiver un annotateur (suppression logique)
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Utilisateur non trouvé !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/users";
        }

        // Suppression logique : on désactive le compte
        user.setEnabled(false);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "✅ L'annotateur '" + user.getUsername() + "' a été désactivé. Ses annotations sont conservées.");
        redirectAttributes.addFlashAttribute("success", true);

        return "redirect:/admin/users";
    }

    // Réactiver un annotateur
    @PostMapping("/users/enable/{id}")
    public String enableUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Utilisateur non trouvé !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/users";
        }

        user.setEnabled(true);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "✅ L'annotateur '" + user.getUsername() + "' a été réactivé !");
        redirectAttributes.addFlashAttribute("success", true);

        return "redirect:/admin/users";
    }

    // Afficher le formulaire de modification
    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Utilisateur non trouvé !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);
        return "admin/users-edit";
    }

    // Modifier un annotateur
    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Long id,
                           @RequestParam(required = false) String firstName,
                           @RequestParam(required = false) String lastName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String password,
                           RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Utilisateur non trouvé !");
            redirectAttributes.addFlashAttribute("success", false);
            return "redirect:/admin/users";
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);

        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "✅ L'annotateur a été modifié avec succès !");
        redirectAttributes.addFlashAttribute("success", true);

        return "redirect:/admin/users";
    }

    // Exporter les annotations d'un dataset au format CSV
    @GetMapping("/datasets/{id}/export/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        Dataset dataset = datasetRepository.findById(id).orElse(null);
        if (dataset == null) {
            return ResponseEntity.notFound().build();
        }

        // Récupérer toutes les annotations du dataset
        List<Annotation> annotations = annotationRepository.findByTextUnit_Dataset(dataset);

        // Construire le contenu CSV
        StringBuilder csv = new StringBuilder();
        csv.append("id,texte,classe,annotateur,date_annotation\n");

        for (Annotation annotation : annotations) {
            TextUnit textUnit = annotation.getTextUnit();
            csv.append(textUnit.getId()).append(",");
            csv.append("\"").append(textUnit.getText1().replace("\"", "\"\"")).append("\",");
            csv.append(annotation.getSelectedClass()).append(",");
            csv.append(annotation.getAnnotator().getUsername()).append(",");
            csv.append(annotation.getCreatedAt()).append("\n");
        }

        // Retourner le fichier CSV
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("filename", "annotations_dataset_" + dataset.getName() + ".csv");

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
    // Dashboard Admin - Statistiques
    @GetMapping("/stats")
    public String stats(Model model) {
        // Statistiques globales
        long totalDatasets = datasetRepository.count();
        long totalAnnotators = userRepository.findByRolesName("ROLE_ANNOTATOR").stream()
                .filter(User::isEnabled)
                .count();
        long totalAnnotations = annotationRepository.count();
        long totalTasks = annotationTaskRepository.count();
        long completedTasks = annotationTaskRepository.countByCompleted(true);
        int globalProgress = totalTasks > 0 ? (int) (completedTasks * 100 / totalTasks) : 0;

        // Statistiques par annotateur
        List<User> annotators = userRepository.findByRolesName("ROLE_ANNOTATOR");
        List<com.example.annotation_platform.controller.AnnotatorStat> annotatorStats = new ArrayList<>();
        for (User annotator : annotators) {
            long userTasks = annotationTaskRepository.countByAnnotator(annotator);
            long userCompleted = annotationTaskRepository.countByAnnotatorAndCompleted(annotator, true);
            int progress = userTasks > 0 ? (int) (userCompleted * 100 / userTasks) : 0;

            com.example.annotation_platform.controller.AnnotatorStat stat = new AnnotatorStat();
            stat.setUsername(annotator.getUsername());
            stat.setTotalTasks((int) userTasks);
            stat.setCompletedTasks((int) userCompleted);
            stat.setProgress(progress);
            annotatorStats.add(stat);
        }

        // Statistiques par dataset
        List<Dataset> datasets = datasetRepository.findAll();
        List<DatasetStat> datasetStats = new ArrayList<>();
        for (Dataset dataset : datasets) {
            long datasetTasks = annotationTaskRepository.countByDataset(dataset);
            long datasetCompleted = annotationTaskRepository.countByDatasetAndCompleted(dataset, true);
            int progress = datasetTasks > 0 ? (int) (datasetCompleted * 100 / datasetTasks) : 0;

            DatasetStat stat = new DatasetStat();
            stat.setName(dataset.getName());
            stat.setType(dataset.getType());
            stat.setTotalTasks((int) datasetTasks);
            stat.setCompletedTasks((int) datasetCompleted);
            stat.setProgress(progress);
            datasetStats.add(stat);
        }

        model.addAttribute("totalDatasets", totalDatasets);
        model.addAttribute("totalAnnotators", totalAnnotators);
        model.addAttribute("totalAnnotations", totalAnnotations);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("globalProgress", globalProgress);
        model.addAttribute("annotatorStats", annotatorStats);
        model.addAttribute("datasetStats", datasetStats);

        return "admin/stats";
    }
    @Autowired
    private SpammerDetectionService spammerDetectionService;

    @GetMapping("/spammers")
    public String detectSpammers(Model model) {
        List<SpammerDetectionResult> results = spammerDetectionService.detectSpammers();
        List<User> randomAnnotators = spammerDetectionService.detectRandomAnnotators();

        model.addAttribute("results", results);
        model.addAttribute("randomAnnotators", randomAnnotators);
        model.addAttribute("hasSuspicious", results.stream().anyMatch(SpammerDetectionResult::isSuspicious));

        return "admin/spammers";  // ← Doit correspondre au nom du fichier
    }
    @Autowired
    private KappaService kappaService;
    @GetMapping("/datasets/{id}/kappa")
    public String showKappa(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElse(null);
        if (dataset == null) {
            return "redirect:/admin/datasets";
        }

        double kappa = kappaService.calculateFleissKappa(dataset);
        String interpretation = kappaService.interpretKappa(kappa);
        String colorClass = kappaService.getKappaColor(kappa);

        model.addAttribute("dataset", dataset);
        model.addAttribute("kappa", kappa);
        model.addAttribute("interpretation", interpretation);
        model.addAttribute("colorClass", colorClass);

        return "admin/kappa";
    }
}