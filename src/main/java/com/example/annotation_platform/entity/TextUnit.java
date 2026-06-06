package com.example.annotation_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "text_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000, nullable = false)
    private String text1;

    @Column(length = 2000)
    private String text2;

    @ManyToOne
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @Transient
    private int completedCount;

    public TextUnit(String text1, String text2, Dataset dataset) {
        this.text1 = text1;
        this.text2 = text2;
        this.dataset = dataset;
    }


}