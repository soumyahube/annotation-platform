package com.example.annotation_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "datasets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String classes;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL)
    private List<TextUnit> textUnits;

    public Dataset(String name, String description, String type, String classes) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.classes = classes;
        this.textUnits = new ArrayList<>();
    }
}