package com.example.annotation_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "annotation_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "text_unit_id", nullable = false)
    private TextUnit textUnit;

    @ManyToOne
    @JoinColumn(name = "annotator_id", nullable = false)
    private User annotator;

    @ManyToOne
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    private boolean completed = false;

    public AnnotationTask(TextUnit textUnit, User annotator, Dataset dataset) {
        this.textUnit = textUnit;
        this.annotator = annotator;
        this.dataset = dataset;
        this.completed = false;
    }
}