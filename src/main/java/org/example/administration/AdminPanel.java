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
    public void deleteCategory(int index){
        if(index<categories.size()) {
            categories.remove(index);
        }
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
    public void deleteQuestion(int indexCategory,int indexQuestion){
        List<Question>questions=categories.get(indexCategory).getQuestionList();
        questions.remove(indexQuestion);
        categories.get(indexCategory).setQuestionList(questions);
    }
    public void editQuestionInCategory(int indexCategory,int indexQuestion,String newContent, List<String> newOptions, int newCorrectIndex, int newPoints){
        List<Question>questions=categories.get(indexCategory).getQuestionList();
        questions.get(indexQuestion).edit(newContent,newOptions,newCorrectIndex,newPoints);
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





    //Шаблон при старте рпограммы
    public void generateTemplateCategories() {

        List<Category> categories = new ArrayList<>();

        // ---------- Категория 1 ----------
        List<Question> q1 = new ArrayList<>();

        q1.add(new Question(
                "2 + 2 = ?",
                List.of("3", "4", "5"),
                1,
                1
        ));

        q1.add(new Question(
                "5 * 3 = ?",
                List.of("10", "15", "20"),
                1,
                1
        ));

        q1.add(new Question(
                "9 - 4 = ?",
                List.of("3", "4", "5"),
                2,
                1
        ));

        categories.add(new Category(
                "Простая математика",
                3,
                q1
        ));

        // ---------- Категория 2 ----------
        List<Question> q2 = new ArrayList<>();

        q2.add(new Question(
                "10 / 2 = ?",
                List.of("2", "5", "10"),
                1,
                1
        ));

        q2.add(new Question(
                "7 + 6 = ?",
                List.of("12", "13", "14"),
                1,
                1
        ));

        q2.add(new Question(
                "8 * 2 = ?",
                List.of("14", "16", "18"),
                1,
                1
        ));

        categories.add(new Category(
                "Средняя математика",
                3,
                q2
        ));

        // ---------- Категория 3 ----------
        List<Question> q3 = new ArrayList<>();

        q3.add(new Question(
                "3 * 3 = ?",
                List.of("6", "9", "12"),
                1,
                1
        ));

        q3.add(new Question(
                "12 - 5 = ?",
                List.of("6", "7", "8"),
                1,
                1
        ));

        q3.add(new Question(
                "4 + 7 = ?",
                List.of("10", "11", "12"),
                1,
                1
        ));

        categories.add(new Category(
                "Еще математика",
                3,
                q3
        ));

        tested.setCategories(categories);

        System.out.println("Шаблонные категории созданы");
    }
}