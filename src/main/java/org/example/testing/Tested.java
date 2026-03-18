package org.example.testing;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.models.Category;
import org.example.models.dao.CategoryDAO;
import org.example.testing.question.Question;

import java.util.List;
import java.util.Scanner;

@Data
@NoArgsConstructor
public class Tested {

    private String name = "Тестирование";
    private final Scanner scanner = new Scanner(System.in);

    // 🔹 Загружаем категории из БД
    public List<Category> getCategories() {
        CategoryDAO dao = new CategoryDAO();
        return dao.findAllWithQuestions();
    }

    // 🔹 Общая сумма баллов
    public int getAllPoints() {
        return getCategories().stream()
                .mapToInt(Category::getMaxPoints)
                .sum();
    }

    // 🔹 Выбор категории пользователем
    public Category chooseCategory() {
        List<Category> categories = getCategories();

        if (categories.isEmpty()) {
            System.out.println("❌ Категории не найдены в базе данных.");
            return null;
        }

        System.out.println("\n📚 Выберите категорию:");
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            System.out.println((i + 1) + ". " + c.getName() +
                    " (Вопросов: " + c.getQuestions().size() +
                    ", Баллов: " + c.getMaxPoints() + ")");
        }

        System.out.print("Введите номер категории: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice < 1 || choice > categories.size()) {
            System.out.println("❌ Неверный номер категории.");
            return null;
        }

        return categories.get(choice - 1);
    }

    // 🔹 Запуск тестирования (восстановленный метод)
    public void startCategory() {
        Category category = chooseCategory();

        if (category == null) {
            return;
        }

        if (category.getQuestions() == null || category.getQuestions().isEmpty()) {
            System.out.println("❌ В этой категории нет вопросов.");
            return;
        }

        System.out.println("\n📝 Категория: " + category.getName());
        System.out.println("Всего вопросов: " + category.getQuestions().size());
        System.out.println("Максимальный балл: " + category.getMaxPoints());
        System.out.println("─────────────────────────────────");

        int userPoints = 0;

        for (int i = 0; i < category.getQuestions().size(); i++) {
            Question q = category.getQuestions().get(i);

            System.out.println("\n❓ Вопрос " + (i + 1) + ": " + q.getContent());

            for (int j = 0; j < q.getOptions().size(); j++) {
                System.out.println((j + 1) + ". " + q.getOptions().get(j));
            }

            System.out.print("Ваш ответ (номер): ");
            int answer = scanner.nextInt() - 1;
            scanner.nextLine();

            if (answer == q.getCorrectOptionIndex()) {
                userPoints += q.getPoints();
                System.out.println("✅ Правильно!");
            } else {
                System.out.println("❌ Неправильно.");
            }
        }

        System.out.println("\n─────────────────────────────────");
        System.out.println("🎉 Тест завершён!");
        System.out.println("Ваши баллы: " + userPoints + " из " + category.getMaxPoints());

        double percent = (double) userPoints / category.getMaxPoints();
        int grade;
        if (percent >= 0.9) grade = 5;
        else if (percent >= 0.7) grade = 4;
        else if (percent >= 0.5) grade = 3;
        else grade = 2;

        System.out.println("Оценка: " + grade);
        System.out.println("─────────────────────────────────");
    }
}