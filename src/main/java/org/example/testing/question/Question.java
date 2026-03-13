package org.example.testing.question;

import java.util.List;

public class Question {
    private String content; // текст вопроса
    private List<String> options; // варианты ответов
    private int correctOptionIndex; // индекс правильного ответа
    private boolean isResolved; // отвечен вопрос или нет
    private int points; // баллы за правильный ответ

    public Question() {
    }

    public Question(String content, List<String> options, int correctOptionIndex, int points) {
        this.content = content;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.points = points;
        this.isResolved = false; // по умолчанию не отвечен
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    // Проверка ответа
    public boolean checkAnswer(int userChoiceIndex) {
        if (userChoiceIndex < 0 || userChoiceIndex >= options.size()) {
            return false;
        }
        return userChoiceIndex == correctOptionIndex;
    }

    // Подсчет очков
    public int countPoints(int userChoiceIndex) {
        if (checkAnswer(userChoiceIndex)) {
            isResolved = true;
            return points;
        }
        return 0;
    }

    // Редактировать
    public void edit(String newContent, List<String> newOptions, int newCorrectIndex, int newPoints) {
        this.content = newContent;
        this.options = newOptions;
        this.correctOptionIndex = newCorrectIndex;
        this.points = newPoints;
    }

    public void display() {
        System.out.println("Вопрос: " + content);
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ". " + options.get(i));
            }
        }
        System.out.println("Стоимость: " + points);
    }
    //Просто для проверки
//    public static void main(String[] args) {
//
//        String[] options = {"A. Да", "B. Нет", "C. Не знаю"};
//        Question q = new Question("Вопроссс?", List.of(options), 1, 5);
//        q.display();
//
//        System.out.println("\nПроверка ответов:");
//        System.out.println("Ответ A (индекс 0): " + q.checkAnswer(0));
//        System.out.println("Ответ B (индекс 1): " + q.checkAnswer(1));
//        System.out.println("Очки за ответ B: " + q.countPoints(1));
//        System.out.println("Вопрос отвечен? " + q.isResolved());
//    }
}