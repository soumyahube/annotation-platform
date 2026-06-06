package com.example.annotation_platform.repository;

import com.example.annotation_platform.entity.AnnotationTask;
import com.example.annotation_platform.entity.Dataset;
import com.example.annotation_platform.entity.TextUnit;
import com.example.annotation_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnotationTaskRepository extends JpaRepository<AnnotationTask, Long> {
    List<AnnotationTask> findByAnnotatorAndCompleted(User annotator, boolean completed);
    List<AnnotationTask> findByDataset(Dataset dataset);
    long countByDataset(Dataset dataset);
    long countByDatasetAndCompleted(Dataset dataset, boolean completed);
    long countByAnnotatorAndDataset(User annotator, Dataset dataset);
    long countByAnnotatorAndDatasetAndCompleted(User annotator, Dataset dataset, boolean completed);
    List<AnnotationTask> findByAnnotatorIdAndDatasetIdAndCompletedFalse(Long annotatorId, Long datasetId);
    Optional<AnnotationTask> findByTextUnitAndAnnotator(TextUnit textUnit, User annotator);
    long count();
    long countByCompleted(boolean completed);
    long countByAnnotator(User annotator);
    long countByAnnotatorAndCompleted(User annotator, boolean completed);
}
