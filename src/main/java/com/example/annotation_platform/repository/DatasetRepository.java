package com.example.annotation_platform.repository;

import com.example.annotation_platform.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
}