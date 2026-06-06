package com.example.annotation_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "annotations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "text_unit_id", nullable = false)
    private TextUnit textUnit;

    @ManyToOne
    @JoinColumn(name = "annotator_id", nullable = false)
    private User annotator;

    @Column(nullable = false)
    private String selectedClass;

    private LocalDateTime createdAt;

    public Annotation(TextUnit textUnit, User annotator, String selectedClass) {
        this.textUnit = textUnit;
        this.annotator = annotator;
        this.selectedClass = selectedClass;
        this.createdAt = LocalDateTime.now();
    }
}