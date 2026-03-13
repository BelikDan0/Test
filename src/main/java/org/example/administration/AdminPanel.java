package org.example.administration;

import lombok.Data;
import org.example.testing.Tested;
import org.example.testing.category.Category;
import org.example.testing.question.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
public class AdminPanel {

    private Tested tested;
    private List<Category> categories = new ArrayList<>();
    private Scanner scanner = new Scanner(System.in);

    public AdminPanel(Tested tested) {
        this.tested = tested;
    }

    public void addCategory() {

        System.out.println("Введите название категории");
        String nameCategory = scanner.nextLine();

        List<Question> questions = new ArrayList<>();

        while (true) {
            System.out.println("Добавить вопрос? 1 - да, 0 - нет");
            int choose = scanner.nextInt();
            scanner.nextLine();

            if (choose == 0) {
                break;
            }

            questions.add(addQuestion());
        }

        int allPoints = 0;
        for (Question q : questions) {
            allPoints += q.getPoints();
        }

        Category category = new Category(nameCategory, allPoints, questions);
        categories.add(category);
    }

    public Question addQuestion() {

        System.out.println("Введите текст вопроса");
        String name = scanner.nextLine();

        List<String> options = new ArrayList<>();

        System.out.println("Введите варианты ответа (/exit чтобы закончить)");

        while (true) {
            String option = scanner.nextLine();

            if (option.equals("/exit")) {
                break;
            }

            options.add(option);
        }

        System.out.println("Введите номер правильного ответа");
        int correctIndex = scanner.nextInt() - 1;
        scanner.nextLine();

        System.out.println("Введите сколько баллов за вопрос");
        int points = scanner.nextInt();
        scanner.nextLine();

        return new Question(name, options, correctIndex, points);
    }

    public void menuAdmin() {

        System.out.println("1 - Добавить категорию\n2 - Выйти");

        int menu = scanner.nextInt();
        scanner.nextLine();

        if (menu < 1 || menu > 2) {
            System.out.println("Нет такого выбора");
            return;
        }

        switch (menu) {

            case 1:
                addCategory();
                tested.setCategories(categories);
                break;

            case 2:
                return;
        }
    }
}