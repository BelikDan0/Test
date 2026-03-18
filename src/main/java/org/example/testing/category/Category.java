package org.example.testing.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.models.Question;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
@AllArgsConstructor
public class Category {

    private String name;
    private int maxPoints; // максимальные баллы за категорию
    private List<Question> questionList;
    private boolean isFinished;
    private boolean isActive;

    private int currentQuestionIndex = 0;

    // Конструктор по умолчанию
    public Category() {
        this.questionList = new ArrayList<>();
        this.maxPoints = 0;
        this.isFinished = false;
        this.isActive = false;
    }

    // Конструктор с параметрами
    public Category(String name, int maxPoints, List<Question> questionList) {
        this.name = name;
        this.maxPoints = maxPoints;
        this.questionList = questionList;
        this.isFinished = false;
        this.isActive = false;
    }

    // Показ информации о категории
    public void showInfo() {
        System.out.println("Name: " + name);
        System.out.println("Max points: " + maxPoints);
        System.out.println("isFinished: " + isFinished);
        System.out.println("isActive: " + isActive);
        System.out.println("Questions:");
        for (int i = 0; i < questionList.size(); i++) {
            System.out.println((i + 1) + ". " + questionList.get(i).getContent());
        }
    }

    // Прохождение категории
    public void start() {
        if (questionList.isEmpty()) {
            System.out.println("Ошибка: в категории нет вопросов");
            return;
        }

        this.isActive = true;
        System.out.println("Категория '" + name + "' начата. Всего вопросов: " + questionList.size());

        int userPoints = 0; // Локальный счетчик набранных баллов

        Scanner scanner = new Scanner(System.in);

        for (int i = 0; i < questionList.size(); i++) {
            Question q = questionList.get(i);

            System.out.println("\nВопрос " + (i + 1) + ": " + q.getContent());
            List<String> options = q.getOptions();
            for (int j = 0; j < options.size(); j++) {
                System.out.println((j + 1) + ". " + options.get(j));
            }

            System.out.print("Введите номер ответа: ");
            int answer = scanner.nextInt() - 1;

            if (answer == q.getCorrectOptionIndex()) {
                userPoints += q.getPoints();
            }
        }

        System.out.println("\nВы набрали " + userPoints + " баллов из " + maxPoints);

        this.isFinished = true;
        this.isActive = false;
    }

    // Для запуска категории по одному вопросу (необязательно)
    public void setActive(boolean active) {
        this.isActive = active;
        this.currentQuestionIndex = 0;
        System.out.println("Категория '" + name + "' начата.");
        if (!questionList.isEmpty()) {
            System.out.println("Первый вопрос: " + questionList.get(currentQuestionIndex).getContent());
        }
    }

    // Сохранение категории в файл
    public void saveInFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(name + ";" + maxPoints);
            writer.newLine();
            for (Question q : questionList) {
                writer.write(q.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сохранения файла");
        }
    }
}