package com.example.annotation_platform.repository;

import com.example.annotation_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouver un utilisateur par son nom d'utilisateur
     */
    Optional<User> findByUsername(String username);

    /**
     * Vérifier si un nom d'utilisateur existe déjà
     */
    boolean existsByUsername(String username);

    /**
     * Trouver tous les utilisateurs ayant un rôle spécifique
     * Exemple: findByRolesName("ROLE_ANNOTATOR")
     */
    List<User> findByRolesName(String roleName);

    /**
     * Trouver tous les utilisateurs dont le compte est activé
     */
    List<User> findByEnabledTrue();

    /**
     * Trouver tous les utilisateurs par ordre alphabétique
     */
    List<User> findAllByOrderByUsernameAsc();
}