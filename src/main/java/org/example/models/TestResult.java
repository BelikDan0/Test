package org.example.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Data
@NoArgsConstructor
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser user;

    @Column(nullable = false)
    private String categoryName;  // Название категории (для отображения)

    @Column(nullable = false)
    private int userPoints;       // Баллы пользователя

    @Column(nullable = false)
    private int maxPoints;        // Максимально возможные баллы

    @Column(nullable = false)
    private int grade;            // Оценка (2-5)

    @Column(nullable = false)
    private LocalDateTime completedAt;  // Дата завершения

    // Конструктор для удобства
    public TestResult(TelegramUser user, String categoryName, int userPoints, int maxPoints, int grade) {
        this.user = user;
        this.categoryName = categoryName;
        this.userPoints = userPoints;
        this.maxPoints = maxPoints;
        this.grade = grade;
        this.completedAt = LocalDateTime.now();
    }

    // 🔹 Вспомогательный метод: процент правильных ответов
    public double getPercent() {
        if (maxPoints == 0) return 0;
        return (double) userPoints / maxPoints * 100;
    }

    // 🔹 Форматированный вывод результата
    public String toFormattedString() {
        return String.format("📋 %s\n" +
                        "   💯 %d/%d баллов (%.1f%%)\n" +
                        "   🎓 Оценка: %d\n" +
                        "   🕐 %s",
                categoryName,
                userPoints, maxPoints,
                getPercent(),
                grade,
                completedAt.toString().substring(0, 16)  // "2024-03-18 10:30"
        );
    }
}