package org.example.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", length = 500)
    private List<String> options = new ArrayList<>();

    @Column(nullable = false)
    private int correctOptionIndex;

    @Column(nullable = false)
    private int points;

    @Column
    private boolean isResolved = false;

    // Конструктор
    public Question(String content, List<String> options, int correctOptionIndex, int points) {
        this.content = content;
        this.options = options != null ? options : new ArrayList<>();
        this.correctOptionIndex = correctOptionIndex;
        this.points = points;
    }

    // 🔹 Методы из старого POJO (для логики проверки)

    // Проверка ответа
    public boolean checkAnswer(int userChoiceIndex) {
        if (options == null || userChoiceIndex < 0 || userChoiceIndex >= options.size()) {
            return false;
        }
        return userChoiceIndex == correctOptionIndex;
    }

    // Подсчёт очков
    public int countPoints(int userChoiceIndex) {
        if (checkAnswer(userChoiceIndex)) {
            this.isResolved = true;
            return points;
        }
        return 0;
    }

    // Редактирование вопроса
    public void edit(String newContent, List<String> newOptions, int newCorrectIndex, int newPoints) {
        this.content = newContent;
        this.options = newOptions != null ? newOptions : new ArrayList<>();
        this.correctOptionIndex = newCorrectIndex;
        this.points = newPoints;
    }

    // Отображение в консоли (для отладки)
    public void display() {
        System.out.println("❓ Вопрос: " + content);
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                System.out.println("   " + (i + 1) + ". " + options.get(i));
            }
        }
        System.out.println("💰 Баллы: " + points);
    }
}