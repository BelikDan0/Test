package org.example.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int maxPoints;

    @Column
    private boolean isFinished = false;

    @Column
    private boolean isActive = false;

    // Однонаправленная связь: Category → Questions
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private List<Question> questions = new ArrayList<>();

    public Category(String name, int maxPoints) {
        this.name = name;
        this.maxPoints = maxPoints;
    }

    // Методы для удобства работы
    public void addQuestion(Question question) {
        questions.add(question);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
    }
}