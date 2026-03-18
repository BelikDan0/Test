package org.example.administration;

import lombok.Data;

import org.example.models.Category;
import org.example.models.Question;
import org.example.models.dao.CategoryDAO;
import org.example.testing.Tested;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
public class AdminPanel {

    private Tested tested;
    private CategoryDAO categoryDAO = new CategoryDAO();
    private Scanner scanner = new Scanner(System.in);

    public AdminPanel(Tested tested) {
        this.tested = tested;
    }

    public void addCategory() {
        System.out.println("📝 Введите название категории:");
        String name = scanner.nextLine();

        List<Question> questions = new ArrayList<>();

        while (true) {
            System.out.println("➕ Добавить вопрос? 1 - да, 0 - нет");
            int choose = scanner.nextInt();
            scanner.nextLine();

            if (choose == 0) break;
            questions.add(addQuestion());
        }

        int maxPoints = questions.stream().mapToInt(Question::getPoints).sum();

        Category category = new Category(name, maxPoints);
        questions.forEach(category::addQuestion);

        categoryDAO.save(category); // ✅ Сохранение в БД
        System.out.println("✅ Категория '" + name + "' сохранена в БД!");
    }

    public void showCategories() {
        List<Category> categories = categoryDAO.findAllWithQuestions();
        if (categories.isEmpty()) {
            System.out.println("Категорий нет.");
            return;
        }

        System.out.println("\n📚 Список категорий:");
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            System.out.println((i + 1) + ". " + c.getName() +
                    " (Вопросов: " + c.getQuestions().size() +
                    ", Баллов: " + c.getMaxPoints() + ")");
        }
    }

    public void deleteCategory() {
        showCategories();
        System.out.println("Введите номер категории для удаления:");
        int index = scanner.nextInt() - 1;

        List<Category> categories = categoryDAO.findAllWithQuestions();
        if (index >= 0 && index < categories.size()) {
            Category toDelete = categories.get(index);
            categoryDAO.delete(toDelete); // ✅ Удаление из БД (вопросы удалятся каскадом)
            System.out.println("✅ Категория удалена.");
        } else {
            System.out.println("❌ Неверный номер.");
        }
    }

    public void menuAdmin() {
        while (true) {
            System.out.println("\n🛠️ Админ-панель:");
            System.out.println("1 - Добавить категорию");
            System.out.println("2 - Показать категории");
            System.out.println("3 - Удалить категорию");
            System.out.println("0 - Выход");

            int menu = scanner.nextInt();
            scanner.nextLine();

            switch (menu) {
                case 1 -> addCategory();
                case 2 -> showCategories();
                case 3 -> deleteCategory();
                case 0 -> { return; }
                default -> System.out.println("❌ Нет такого выбора");
            }
        }
    }

    private Question addQuestion() {
        System.out.println("Введите текст вопроса:");
        String content = scanner.nextLine();

        List<String> options = new ArrayList<>();
        System.out.println("Введите варианты ответа (по одному, /exit для завершения):");
        while (true) {
            String opt = scanner.nextLine();
            if (opt.equals("/exit")) break;
            options.add(opt);
        }

        System.out.println("Введите номер правильного ответа (1-" + options.size() + "):");
        int correctIndex = scanner.nextInt() - 1;

        System.out.println("Введите баллы за вопрос:");
        int points = scanner.nextInt();
        scanner.nextLine();

        return new Question(content, options, correctIndex, points);
    }

    // Шаблонные данные (загружаются в БД при первом запуске)
    public void generateTemplateCategories() {
        if (!categoryDAO.findAll().isEmpty()) {
            System.out.println("⚠️ База данных не пуста, шаблоны не созданы.");
            return;
        }

        Category cat1 = new Category("Простая математика", 3);
        cat1.addQuestion(new Question("2 + 2 = ?", List.of("3", "4", "5"), 1, 1));
        cat1.addQuestion(new Question("5 * 3 = ?", List.of("10", "15", "20"), 1, 1));
        cat1.addQuestion(new Question("9 - 4 = ?", List.of("3", "4", "5"), 2, 1));
        categoryDAO.save(cat1);

        Category cat2 = new Category("Средняя математика", 3);
        cat2.addQuestion(new Question("10 / 2 = ?", List.of("2", "5", "10"), 1, 1));
        cat2.addQuestion(new Question("7 + 6 = ?", List.of("12", "13", "14"), 1, 1));
        cat2.addQuestion(new Question("8 * 2 = ?", List.of("14", "16", "18"), 1, 1));
        categoryDAO.save(cat2);

        System.out.println("✅ Шаблонные категории созданы в БД.");
    }
}