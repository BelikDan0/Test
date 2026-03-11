package org.example.testing.category;

import org.example.testing.question.Question;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Category {

    private String name;
    private Integer points;
    private List<Question> questionList;
    private Boolean isFinished;
    private Boolean isActive;

    private int currentQuestionIndex = 0;

    public Category() {
        questionList = new ArrayList<>();
        points = 0;
        isFinished = false;
        isActive = false;
    }

    public Category(String name, Integer points, List<Question> questionList, Boolean isFinished, Boolean isActive) {
        this.name = name;
        this.points = points;
        this.questionList = questionList;
        this.isFinished = isFinished;
        this.isActive = isActive;
    }

    public void showInfo() {
        System.out.println("Name: " + name);
        System.out.println("Points: " + points);
        System.out.println("isFinished: " + isFinished);
        System.out.println("isActive: " + isActive);
        System.out.println("Questions:");

        for (int i = 0; i < questionList.size(); i++) {
            System.out.println((i + 1) + ". " + questionList.get(i));
        }
    }

    public void start() {
        if (questionList.isEmpty()) {
            System.out.println("Ошибка: в категории нет вопросов");
            return;
        }

        isActive = true;
        isFinished = false;
        currentQuestionIndex = 0;

        System.out.println("Категория '" + name + "' начата.");
        System.out.println("Первый вопрос: " + questionList.get(currentQuestionIndex));
    }

    public Question nextQuestion() {
        if (!isActive) {
            System.out.println("Категория не запущена");
            return null;
        }

        if (currentQuestionIndex < questionList.size() - 1) {
            currentQuestionIndex++;
            return questionList.get(currentQuestionIndex);
        } else {
            System.out.println("Это последний вопрос");
            return null;
        }
    }

    public Question previousQuestion() {
        if (!isActive) {
            System.out.println("Категория не запущена");
            return null;
        }

        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            return questionList.get(currentQuestionIndex);
        } else {
            System.out.println("Это первый вопрос");
            return null;
        }
    }

    public int end() {
        isActive = false;
        isFinished = true;

        System.out.println("Категория завершена.");
        return points;
    }

    public void addQuestion(Question question) {
        questionList.add(question);
    }

    public void removeQuestion(int index) {
        if (index >= 0 && index < questionList.size()) {
            questionList.remove(index);
        } else {
            System.out.println("Неверный индекс вопроса");
        }
    }

    public void saveInFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(name + ";" + points);
            writer.newLine();

            for (Question q : questionList) {
                writer.write(q.toString());
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println("Ошибка сохранения файла");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<Question> getQuestionList() {
        return questionList;
    }

    public void setQuestionList(List<Question> questionList) {
        this.questionList = questionList;
    }

    public Boolean getFinished() {
        return isFinished;
    }

    public void setFinished(Boolean finished) {
        isFinished = finished;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}