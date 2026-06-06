package com.example.annotation_platform.repository;

import com.example.annotation_platform.entity.Annotation;
import com.example.annotation_platform.entity.Dataset;
import com.example.annotation_platform.entity.TextUnit;
import com.example.annotation_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    List<Annotation> findByTextUnit_Dataset(Dataset dataset);
    long count();
    List<Annotation> findByAnnotator(User annotator);
    List<Annotation> findByTextUnit(TextUnit textUnit);
}

