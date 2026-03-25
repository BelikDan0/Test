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
    // Шаблонные данные (загружаются в БД при первом запуске)
    public void generateTemplateCategories() {
        if (!categoryDAO.findAll().isEmpty()) {
            System.out.println("⚠️ База данных не пуста, шаблоны не созданы.");
            return;
        }

        // === 1. Простая математика ===
        Category cat1 = new Category("Простая математика", 5);
        cat1.addQuestion(new Question("2 + 2 = ?", List.of("3", "4", "5", "6"), 1, 1));
        cat1.addQuestion(new Question("5 * 3 = ?", List.of("10", "15", "20", "25"), 1, 1));
        cat1.addQuestion(new Question("9 - 4 = ?", List.of("3", "4", "5", "6"), 2, 1));
        cat1.addQuestion(new Question("10 / 2 = ?", List.of("2", "5", "10", "20"), 1, 1));
        cat1.addQuestion(new Question("7 + 8 = ?", List.of("13", "14", "15", "16"), 2, 1));
        categoryDAO.save(cat1);

        // === 2. Средняя математика ===
        Category cat2 = new Category("Средняя математика", 6);
        cat2.addQuestion(new Question("12 * 4 = ?", List.of("36", "42", "48", "54"), 2, 1));
        cat2.addQuestion(new Question("100 / 25 = ?", List.of("2", "4", "5", "10"), 1, 1));
        cat2.addQuestion(new Question("15 + 27 = ?", List.of("40", "41", "42", "43"), 2, 1));
        cat2.addQuestion(new Question("8² = ?", List.of("16", "32", "64", "128"), 2, 1));
        cat2.addQuestion(new Question("√49 = ?", List.of("5", "6", "7", "8"), 2, 1));
        cat2.addQuestion(new Question("3³ = ?", List.of("9", "18", "27", "81"), 2, 1));
        categoryDAO.save(cat2);

        // === 3. Основы программирования ===
        Category cat3 = new Category("Основы программирования", 8);
        cat3.addQuestion(new Question("Что такое переменная?",
                List.of("Функция", "Ячейка памяти для хранения данных", "Команда", "Оператор"), 1, 1));
        cat3.addQuestion(new Question("Какой тип данных хранит целые числа?",
                List.of("String", "Double", "int", "boolean"), 2, 1));
        cat3.addQuestion(new Question("Что делает оператор '+' в Java?",
                List.of("Вычитание", "Сложение", "Умножение", "Деление"), 1, 1));
        cat3.addQuestion(new Question("Как начинается выполнение программы в Java?",
                List.of("main()", "start()", "run()", "init()"), 0, 2));
        cat3.addQuestion(new Question("Что такое цикл?",
                List.of("Условие", "Повторение блока кода", "Функция", "Переменная"), 1, 1));
        cat3.addQuestion(new Question("Какой ключевой слово используется для создания класса?",
                List.of("class", "def", "function", "struct"), 0, 1));
        cat3.addQuestion(new Question("Что возвращает логический тип boolean?",
                List.of("Число", "Текст", "true/false", "Объект"), 2, 1));
        cat3.addQuestion(new Question("Как обозначается комментарий в Java?",
                List.of("#", "//", "--", "<!>"), 1, 1));
        categoryDAO.save(cat3);

        // === 4. Английский язык (базовый) ===
        Category cat4 = new Category("Английский язык (базовый)", 6);
        cat4.addQuestion(new Question("Переведите: 'Hello'",
                List.of("Пока", "Привет", "Спасибо", "Пожалуйста"), 1, 1));
        cat4.addQuestion(new Question("Как будет 'Книга' на английском?",
                List.of("Pen", "Book", "Table", "Chair"), 1, 1));
        cat4.addQuestion(new Question("Выберите правильный артикль: ___ apple",
                List.of("a", "an", "the", "-"), 1, 1));
        cat4.addQuestion(new Question("Глагол 'to be' в настоящем времени для 'I':",
                List.of("is", "are", "am", "be"), 2, 2));
        cat4.addQuestion(new Question("Переведите: 'Good morning'",
                List.of("Добрый вечер", "Доброе утро", "Спокойной ночи", "Привет"), 1, 1));
        cat4.addQuestion(new Question("Множественное число от 'child':",
                List.of("childs", "children", "childes", "child"), 1, 1));
        categoryDAO.save(cat4);

        // === 5. География ===
        Category cat5 = new Category("География", 7);
        cat5.addQuestion(new Question("Столица России?",
                List.of("Санкт-Петербург", "Москва", "Новосибирск", "Казань"), 1, 1));
        cat5.addQuestion(new Question("Самый большой океан?",
                List.of("Атлантический", "Индийский", "Тихий", "Северный Ледовитый"), 2, 1));
        cat5.addQuestion(new Question("На каком континенте находится Египет?",
                List.of("Азия", "Европа", "Африка", "Австралия"), 2, 1));
        cat5.addQuestion(new Question("Самая длинная река в мире?",
                List.of("Амазонка", "Нил", "Янцзы", "Миссисипи"), 1, 1));
        cat5.addQuestion(new Question("Гора Эверест находится в:",
                List.of("Альпах", "Андах", "Гималаях", "Кавказе"), 2, 1));
        cat5.addQuestion(new Question("Какая страна имеет форму сапога?",
                List.of("Испания", "Франция", "Италия", "Греция"), 2, 1));
        cat5.addQuestion(new Question("Самая маленькая страна в мире?",
                List.of("Монако", "Ватикан", "Сан-Марино", "Лихтенштейн"), 1, 1));
        categoryDAO.save(cat5);

        // === 6. История России ===
        Category cat6 = new Category("История России", 8);
        cat6.addQuestion(new Question("В каком году произошло Крещение Руси?",
                List.of("988", "1000", "1147", "1242"), 0, 2));
        cat6.addQuestion(new Question("Кто был первым царём России?",
                List.of("Пётр I", "Иван Грозный", "Николай II", "Александр Невский"), 1, 1));
        cat6.addQuestion(new Question("В каком году началась Великая Отечественная война?",
                List.of("1939", "1941", "1943", "1945"), 1, 1));
        cat6.addQuestion(new Question("Кто основал Санкт-Петербург?",
                List.of("Екатерина II", "Пётр I", "Иван III", "Александр I"), 1, 1));
        cat6.addQuestion(new Question("В каком году произошёл распад СССР?",
                List.of("1989", "1990", "1991", "1992"), 2, 1));
        cat6.addQuestion(new Question("Кто написал 'Войну и мир'?",
                List.of("Достоевский", "Толстой", "Пушкин", "Чехов"), 1, 1));
        cat6.addQuestion(new Question("Битва на Куликовом поле произошла в:",
                List.of("1240", "1380", "1480", "1552"), 1, 1));
        cat6.addQuestion(new Question("Кто был первым президентом РФ?",
                List.of("В. Путин", "Б. Ельцин", "М. Горбачёв", "Д. Медведев"), 1, 1));
        categoryDAO.save(cat6);

        // === 7. Наука и природа ===
        Category cat7 = new Category("Наука и природа", 7);
        cat7.addQuestion(new Question("Сколько планет в Солнечной системе?",
                List.of("7", "8", "9", "10"), 1, 1));
        cat7.addQuestion(new Question("Какой газ мы вдыхаем?",
                List.of("Азот", "Кислород", "Углекислый газ", "Водород"), 1, 1));
        cat7.addQuestion(new Question("Самое быстрое животное на суше?",
                List.of("Лев", "Гепард", "Антилопа", "Лошадь"), 1, 1));
        cat7.addQuestion(new Question("Из чего состоит вода?",
                List.of("H2O", "CO2", "NaCl", "O2"), 0, 2));
        cat7.addQuestion(new Question("Какая планета ближе всего к Солнцу?",
                List.of("Венера", "Земля", "Меркурий", "Марс"), 2, 1));
        cat7.addQuestion(new Question("Сколько ног у паука?",
                List.of("4", "6", "8", "10"), 2, 1));
        cat7.addQuestion(new Question("Что такое фотосинтез?",
                List.of("Дыхание растений", "Процесс получения энергии от света", "Рост корней", "Цветение"), 1, 1));
        categoryDAO.save(cat7);

        // === 8. Литература ===
        Category cat8 = new Category("Литература", 6);
        cat8.addQuestion(new Question("Кто написал 'Евгения Онегина'?",
                List.of("Лермонтов", "Пушкин", "Гоголь", "Тургенев"), 1, 1));
        cat8.addQuestion(new Question("Главный герой 'Преступления и наказания'?",
                List.of("Обломов", "Раскольников", "Печорин", "Онегин"), 1, 1));
        cat8.addQuestion(new Question("Жанр произведения 'Мёртвые души'?",
                List.of("Роман", "Поэма", "Повесть", "Рассказ"), 1, 1));
        cat8.addQuestion(new Question("Кто автор 'Героя нашего времени'?",
                List.of("Пушкин", "Лермонтов", "Толстой", "Достоевский"), 1, 1));
        cat8.addQuestion(new Question("Как звали собаку в рассказе 'Муму'?",
                List.of("Бобик", "Муму", "Жучка", "Трезор"), 1, 1));
        cat8.addQuestion(new Question("Что такое 'золотой век' русской литературы?",
                List.of("XVII век", "XVIII век", "XIX век", "XX век"), 2, 1));
        categoryDAO.save(cat8);

        // === 9. Логика и смекалка ===
        Category cat9 = new Category("Логика и смекалка", 6);
        cat9.addQuestion(new Question("Что тяжелее: килограмм пуха или килограмм железа?",
                List.of("Пух", "Железо", "Одинаково", "Зависит от влажности"), 2, 1));
        cat9.addQuestion(new Question("Сколько месяцев в году имеют 28 дней?",
                List.of("1", "2", "6", "12"), 3, 1));
        cat9.addQuestion(new Question("Что можно увидеть с закрытыми глазами?",
                List.of("Ничего", "Сон", "Темноту", "Всё вышеперечисленное"), 3, 1));
        cat9.addQuestion(new Question("Если вчера было завтра, то какой сегодня день?",
                List.of("Понедельник", "Среда", "Пятница", "Воскресенье"), 2, 1));
        cat9.addQuestion(new Question("У отца Мэри 5 дочерей: Чача, Чече, Чичи, Чочо. Как зовут пятую?",
                List.of("Чучу", "Мэри", "Чача", "Чече"), 1, 1));
        cat9.addQuestion(new Question("Сколько раз цифра 3 встречается в числах от 1 до 30?",
                List.of("10", "11", "12", "13"), 3, 1));
        categoryDAO.save(cat9);

        System.out.println("✅ Создано 9 шаблонных категорий с 59 вопросами в БД.");
    }
}