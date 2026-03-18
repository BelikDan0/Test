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
    @Column(name = "option_text")
    private List<String> options = new ArrayList<>();

    @Column(nullable = false)
    private int correctOptionIndex;

    @Column(nullable = false)
    private int points;

    @Column
    private boolean isResolved = false;


    public Question(String content, List<String> options, int correctOptionIndex, int points) {
        this.content = content;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.points = points;
    }
}
