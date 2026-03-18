package org.example.bot;

import org.example.models.Category;
import org.example.models.Question;
import org.example.models.TelegramUser;
import org.example.models.TestResult;
import org.example.models.dao.CategoryDAO;
import org.example.models.dao.TelegramUserDao;
import org.example.models.dao.TestResultDAO;
import org.example.user.UserSession;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final TelegramUserDao userDAO = new TelegramUserDao();
    private final TestResultDAO resultDAO = new TestResultDAO();
    private final Map<Long, UserSession> sessions = new HashMap<>();
    private final String ADMIN_PASSWORD = "12345";

    public Bot() {}

    @Override
    public String getBotUsername() {
        return "@test2324251_bot";
    }

    @Override
    public String getBotToken() {
        return "8547457064:AAEO88kSaSIbTIdbhNMwqtFK4cPo9X1B-bg";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // Регистрируем пользователя
        var tgMsg = update.getMessage();
        userDAO.getOrCreate(
                chatId,
                tgMsg.getFrom().getUserName(),
                tgMsg.getFrom().getFirstName(),
                tgMsg.getFrom().getLastName()
        );

        sessions.putIfAbsent(chatId, new UserSession());
        UserSession session = sessions.get(chatId);

        // === Команды ===
        if (text.equals("/start")) {
            showCategories(chatId);
            return;
        }

        if (text.startsWith("/admin")) {
            handleAdminCommand(chatId, text, session);
            return;
        }

        if (text.equals("/results") || text.equals("/grades") || text.equals("/оценки")) {
            showUserResults(chatId, session);
            return;
        }

        if (text.equals("/add_category") || text.equals("/добавить_категорию")) {
            if (!session.isAdminMode()) {
                send(chatId, "🔐 Только для администраторов!");
                return;
            }
            startAddCategoryWizard(chatId, session);
            return;
        }

        if (text.equals("/add_question") || text.equals("/добавить_вопрос")) {
            if (!session.isAdminMode()) {
                send(chatId, "🔐 Только для администраторов!");
                return;
            }
            startAddQuestionWizard(chatId, session);
            return;
        }

        // === Приоритет: обработка мастеров ===
        if (session.isAddingCategory()) {
            handleAddCategoryStep(chatId, text, session);
            return;
        }

        if (session.isAddingQuestion()) {
            handleAddQuestionStep(chatId, text, session);
            return;
        }

        // === Админ-меню ===
        if (session.isAdminMode()) {
            handleAdminMenu(chatId, text, session);
            return;
        }

        // === Тестирование ===
        if (!session.isTesting()) {
            handleCategorySelection(chatId, text, session);
        } else {
            handleAnswerProcessing(chatId, text, session);
        }
    }

    // ==================== КАТЕГОРИИ ====================

    private void showCategories(long chatId) {
        List<Category> categories = categoryDAO.findAllWithQuestions();
        if (categories.isEmpty()) {
            send(chatId, "📭 Категории пока не добавлены.");
            return;
        }

        StringBuilder msg = new StringBuilder("📚 Выберите категорию:\n\n");
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            msg.append(i + 1).append(" - ").append(c.getName())
                    .append(" (").append(c.getQuestions().size()).append(" вопр., ")
                    .append(c.getMaxPoints()).append(" бал.)\n");
        }
        send(chatId, msg.toString());
    }

    private void handleCategorySelection(long chatId, String text, UserSession session) {
        try {
            int index = Integer.parseInt(text) - 1;
            List<Category> categories = categoryDAO.findAllWithQuestions();

            if (index < 0 || index >= categories.size()) {
                send(chatId, "❌ Нет такой категории.");
                return;
            }

            Category category = categories.get(index);
            session.setCategory(category);
            session.setCategoryIndex(index);
            session.setTesting(true);
            session.setCurrentQuestion(0);
            session.setPoints(0);

            send(chatId, "📝 Категория: " + category.getName());
            sendQuestion(chatId, session);

        } catch (NumberFormatException e) {
            send(chatId, "🔢 Введите номер категории.");
        }
    }

    // ==================== ВОПРОСЫ ====================

    private void sendQuestion(long chatId, UserSession session) {
        Category category = session.getCategory();
        if (category == null || category.getQuestions() == null) {
            send(chatId, "❌ Ошибка загрузки вопросов.");
            session.setTesting(false);
            return;
        }

        int qIndex = session.getCurrentQuestion();
        if (qIndex >= category.getQuestions().size()) {
            finishTest(chatId, session);
            return;
        }

        Question q = category.getQuestions().get(qIndex);
        List<String> options = q.getOptions();
        if (options == null) options = new ArrayList<>();

        StringBuilder msg = new StringBuilder();
        msg.append("❓ Вопрос ").append(qIndex + 1).append("\n\n");
        msg.append(q.getContent()).append("\n\n");

        for (int i = 0; i < options.size(); i++) {
            msg.append(i + 1).append(". ").append(options.get(i)).append("\n");
        }

        send(chatId, msg.toString());
    }

    private void handleAnswerProcessing(long chatId, String text, UserSession session) {
        try {
            int answer = Integer.parseInt(text) - 1;
            Question q = session.getCategory().getQuestions().get(session.getCurrentQuestion());

            if (answer == q.getCorrectOptionIndex()) {
                session.setPoints(session.getPoints() + q.getPoints());
                send(chatId, "✅ Правильно!");
            } else {
                send(chatId, "❌ Неправильно.");
            }

            session.setCurrentQuestion(session.getCurrentQuestion() + 1);

            if (session.getCurrentQuestion() >= session.getCategory().getQuestions().size()) {
                finishTest(chatId, session);
            } else {
                sendQuestion(chatId, session);
            }

        } catch (NumberFormatException e) {
            send(chatId, "🔢 Введите номер ответа.");
        }
    }

    private void finishTest(long chatId, UserSession session) {
        Category category = session.getCategory();
        int userPoints = session.getPoints();
        int maxPoints = category.getMaxPoints();
        int grade = calculateGrade(userPoints, maxPoints);

        userDAO.findByChatId(chatId).ifPresent(user -> {
            resultDAO.saveResult(user, category.getName(), userPoints, maxPoints, grade);
        });

        send(chatId, "🎉 Тест завершён!\n\n" +
                "Категория: " + category.getName() +
                "\nБаллы: " + userPoints + " из " + maxPoints +
                "\nОценка: " + grade +
                "\n\n💡 Используйте /results чтобы посмотреть историю");

        session.setTesting(false);
        session.setPoints(0);
        session.setCurrentQuestion(0);

        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        showCategories(chatId);
    }

    private int calculateGrade(int points, int max) {
        if (max == 0) return 2;
        double percent = (double) points / max;
        if (percent >= 0.9) return 5;
        if (percent >= 0.7) return 4;
        if (percent >= 0.5) return 3;
        return 2;
    }

    // ==================== АДМИН ====================

    private void handleAdminCommand(long chatId, String text, UserSession session) {
        String[] parts = text.split(" ");
        if (parts.length < 2) {
            send(chatId, "🔑 Введите пароль: /admin пароль");
            return;
        }
        if (parts[1].equals(ADMIN_PASSWORD)) {
            session.setAdminMode(true);
            send(chatId, "🛠️ Админ-панель:\n\n" +
                    "1 ➕ Добавить вопрос в категорию\n" +
                    "2 📁 Создать новую категорию\n" +
                    "3 📋 Показать категории\n" +
                    "4 🔄 Обновить список\n" +
                    "0 🚪 Выход");
        } else {
            send(chatId, "🚫 Неверный пароль.");
        }
    }

    private void handleAdminMenu(long chatId, String text, UserSession session) {
        try {
            int cmd = Integer.parseInt(text);
            switch (cmd) {
                case 1 -> startAddQuestionWizard(chatId, session);
                case 2 -> startAddCategoryWizard(chatId, session);
                case 3 -> showCategories(chatId);
                case 4 -> {
                    send(chatId, "🔄 Список обновлён");
                    showCategories(chatId);
                }
                case 0 -> {
                    session.setAdminMode(false);
                    send(chatId, "🚪 Выход из админ-панели");
                }
                default -> send(chatId, "❌ Нет такой команды");
            }
        } catch (Exception e) {
            send(chatId, "🔢 Введите номер команды");
        }
    }

    // ==================== МАСТЕР: ДОБАВЛЕНИЕ КАТЕГОРИИ ====================

    private void startAddCategoryWizard(long chatId, UserSession session) {
        session.setAddingCategory(true);
        session.setAddingCategoryStep(0);
        session.setAddingCategoryName(new StringBuilder());
        session.setAddingCategoryQuestions(new ArrayList<>());
        session.setAddingCategoryMaxPoints(0);

        send(chatId, "📁 Создание новой категории\n\n✍️ Введите название категории:");
    }

    private void handleAddCategoryStep(long chatId, String text, UserSession session) {
        int step = session.getAddingCategoryStep();

        switch (step) {
            case 0 -> handleStepCategoryName(chatId, text, session);
            case 1 -> handleStepAskAddQuestions(chatId, text, session);
            case 2 -> {
                if (session.isAddingQuestionInsideCategory()) {
                    handleStepQuestionInsideCategory(chatId, text, session);
                } else {
                    handleStepManageQuestions(chatId, text, session);
                }
            }
        }
    }

    private void handleStepCategoryName(long chatId, String text, UserSession session) {
        if (text.trim().isEmpty()) {
            send(chatId, "❌ Название не может быть пустым. Введите ещё раз:");
            return;
        }
        session.setAddingCategoryName(new StringBuilder(text.trim()));
        session.setAddingCategoryStep(1);
        send(chatId, "✅ Название: \"" + text + "\"\n\n" +
                "➕ Хотите добавить вопросы сразу?\n" +
                "1 - Да, добавить вопросы сейчас\n" +
                "0 - Нет, создать пустую категорию");
    }

    private void handleStepAskAddQuestions(long chatId, String text, UserSession session) {
        if (text.equals("1") || text.equalsIgnoreCase("да")) {
            session.setAddingCategoryStep(2);
            sendAddQuestionInstructions(chatId);
        } else if (text.equals("0") || text.equalsIgnoreCase("нет")) {
            saveNewCategory(session);
            session.setAddingCategory(false);
            send(chatId, "🎉 Категория \"" + session.getAddingCategoryName() + "\" создана!\n" +
                    "Добавить вопросы можно позже через /add_question");
        } else {
            send(chatId, "❌ Введите 1 (да) или 0 (нет):");
        }
    }

    private void handleStepManageQuestions(long chatId, String text, UserSession session) {
        if (text.equals("/done") || text.equals("/готово") || text.equals("0")) {
            saveNewCategory(session);
            session.setAddingCategory(false);
            send(chatId, "🎉 Категория \"" + session.getAddingCategoryName() + "\" создана!\n" +
                    "Вопросов: " + session.getAddingCategoryQuestions().size() + "\n" +
                    "Макс. баллов: " + session.getAddingCategoryMaxPoints());
            return;
        }

        if (text.equals("1") || text.equalsIgnoreCase("да")) {
            session.setAddingQuestionInsideCategory(true);
            session.setAddingQuestionInsideStep(0);
            session.setTempQuestionText(new StringBuilder());
            session.setTempQuestionOptions(new ArrayList<>());
            send(chatId, "✍️ Введите текст вопроса:");
            return;
        }

        if (text.equals("2") || text.equalsIgnoreCase("список")) {
            List<Question> questions = session.getAddingCategoryQuestions();
            if (questions.isEmpty()) {
                send(chatId, "📭 Пока нет добавленных вопросов");
            } else {
                StringBuilder msg = new StringBuilder("📋 Вопросы в категории:\n");
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    msg.append(i + 1).append(". ").append(q.getContent())
                            .append(" (").append(q.getPoints()).append(" бал.)\n");
                }
                send(chatId, msg.toString());
            }
            sendAddQuestionInstructions(chatId);
            return;
        }

        send(chatId, "❌ Неизвестная команда.\n" +
                "1 - Добавить вопрос\n" +
                "2 - Показать список\n" +
                "0 или /done - Завершить создание категории");
    }

    private void sendAddQuestionInstructions(long chatId) {
        send(chatId, "➕ Добавление вопроса:\n" +
                "1 - Добавить вопрос сейчас\n" +
                "2 - Показать список вопросов\n" +
                "0 или /done - Завершить создание категории");
    }

    private void handleStepQuestionInsideCategory(long chatId, String text, UserSession session) {
        int step = session.getAddingQuestionInsideStep();

        switch (step) {
            case 0 -> {
                if (text.trim().isEmpty()) {
                    send(chatId, "❌ Вопрос не может быть пустым. Введите ещё раз:");
                    return;
                }
                session.setTempQuestionText(new StringBuilder(text.trim()));
                session.setAddingQuestionInsideStep(1);
                session.setTempQuestionOptions(new ArrayList<>());
                send(chatId, "📋 Введите варианты ответов (по одному).\n" +
                        "Когда закончите, отправьте: /done");
            }
            case 1 -> {
                if (text.equals("/done") || text.equals("/готово")) {
                    if (session.getTempQuestionOptions().size() < 2) {
                        send(chatId, "❌ Нужно минимум 2 варианта. Добавьте ещё:");
                        return;
                    }
                    StringBuilder msg = new StringBuilder("✅ Варианты:\n");
                    List<String> opts = session.getTempQuestionOptions();
                    for (int i = 0; i < opts.size(); i++) {
                        msg.append(i + 1).append(". ").append(opts.get(i)).append("\n");
                    }
                    msg.append("\n🔢 Номер правильного ответа (1-" + opts.size() + "):");
                    session.setAddingQuestionInsideStep(2);
                    send(chatId, msg.toString());
                    return;
                }
                session.getTempQuestionOptions().add(text.trim());
                send(chatId, "➕ Добавлен: \"" + text + "\"\n" +
                        "Отправьте ещё или /done для завершения");
            }
            case 2 -> {
                try {
                    int answer = Integer.parseInt(text) - 1;
                    List<String> opts = session.getTempQuestionOptions();
                    if (answer < 0 || answer >= opts.size()) {
                        send(chatId, "❌ Введите номер от 1 до " + opts.size() + ":");
                        return;
                    }
                    session.setTempQuestionCorrectIndex(answer);
                    session.setAddingQuestionInsideStep(3);
                    send(chatId, "✅ Правильный: " + opts.get(answer) +
                            "\n💰 Баллы за вопрос (число):");
                } catch (NumberFormatException e) {
                    send(chatId, "🔢 Введите номер цифрой:");
                }
            }
            case 3 -> {
                try {
                    int points = Integer.parseInt(text);
                    if (points < 0) {
                        send(chatId, "❌ Баллы не могут быть отрицательными:");
                        return;
                    }
                    session.setTempQuestionPoints(points);

                    Question newQ = new Question(
                            session.getTempQuestionText().toString(),
                            new ArrayList<>(session.getTempQuestionOptions()),
                            session.getTempQuestionCorrectIndex(),
                            session.getTempQuestionPoints()
                    );
                    session.getAddingCategoryQuestions().add(newQ);
                    session.setAddingCategoryMaxPoints(
                            session.getAddingCategoryMaxPoints() + points
                    );

                    session.setAddingQuestionInsideCategory(false);
                    session.setAddingQuestionInsideStep(0);

                    send(chatId, "✅ Вопрос добавлен в категорию!\n" +
                            "Всего вопросов: " + session.getAddingCategoryQuestions().size() +
                            ", Баллов: " + session.getAddingCategoryMaxPoints() + "\n");
                    sendAddQuestionInstructions(chatId);

                } catch (NumberFormatException e) {
                    send(chatId, "🔢 Введите число баллов:");
                }
            }
        }
    }

    private void saveNewCategory(UserSession session) {
        try {
            Category newCategory = new Category(
                    session.getAddingCategoryName().toString(),
                    session.getAddingCategoryMaxPoints()
            );
            for (Question q : session.getAddingCategoryQuestions()) {
                newCategory.addQuestion(q);
            }
            categoryDAO.save(newCategory);
            System.out.println("✅ Категория сохранена: " + newCategory.getName());
        } catch (Exception e) {
            System.err.println("❌ Ошибка сохранения категории: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== МАСТЕР: ДОБАВЛЕНИЕ ВОПРОСА ====================

    private void startAddQuestionWizard(long chatId, UserSession session) {
        session.setAddingQuestion(true);
        session.setAddingQuestionStep(0);
        session.setAddingQuestionOptions(new ArrayList<>());
        session.setAddingQuestionText(new StringBuilder());

        List<Category> categories = categoryDAO.findAllWithQuestions();
        if (categories.isEmpty()) {
            send(chatId, "❌ Нет категорий для добавления вопроса.\nСначала создайте категорию.");
            session.setAddingQuestion(false);
            return;
        }

        StringBuilder msg = new StringBuilder("📝 Добавление вопроса\n\n📚 Выберите категорию:\n");
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            msg.append(i + 1).append(". ").append(c.getName())
                    .append(" (").append(c.getQuestions().size()).append(" вопросов)\n");
        }
        msg.append("\n🔢 Введите номер категории:");
        send(chatId, msg.toString());
    }

    private void handleAddQuestionStep(long chatId, String text, UserSession session) {
        int step = session.getAddingQuestionStep();
        switch (step) {
            case 0 -> handleStepSelectCategory(chatId, text, session);
            case 1 -> handleStepQuestionText(chatId, text, session);
            case 2 -> handleStepOptions(chatId, text, session);
            case 3 -> handleStepCorrectAnswer(chatId, text, session);
            case 4 -> handleStepPoints(chatId, text, session);
        }
    }

    private void handleStepSelectCategory(long chatId, String text, UserSession session) {
        try {
            int index = Integer.parseInt(text) - 1;
            List<Category> categories = categoryDAO.findAllWithQuestions();
            if (index < 0 || index >= categories.size()) {
                send(chatId, "❌ Нет такой категории. Введите номер ещё раз:");
                return;
            }
            Category category = categories.get(index);
            session.setAddingQuestionCategoryId(category.getId());
            session.setAddingQuestionStep(1);
            send(chatId, "✅ Категория: " + category.getName() +
                    "\n\n✍️ Введите текст вопроса:");
        } catch (NumberFormatException e) {
            send(chatId, "🔢 Введите номер категории цифрой:");
        }
    }

    private void handleStepQuestionText(long chatId, String text, UserSession session) {
        if (text.trim().isEmpty()) {
            send(chatId, "❌ Вопрос не может быть пустым. Введите текст ещё раз:");
            return;
        }
        session.setAddingQuestionText(new StringBuilder(text.trim()));
        session.setAddingQuestionStep(2);
        session.setAddingQuestionOptions(new ArrayList<>());
        send(chatId, "📋 Теперь введите варианты ответов.\n" +
                "Отправляйте по одному варианту в сообщении.\n" +
                "Когда закончите, отправьте: /done");
    }

    private void handleStepOptions(long chatId, String text, UserSession session) {
        if (text.equals("/done") || text.equals("/готово")) {
            if (session.getAddingQuestionOptions().size() < 2) {
                send(chatId, "❌ Нужно минимум 2 варианта ответа. Добавьте ещё:");
                return;
            }
            StringBuilder msg = new StringBuilder("✅ Варианты добавлены:\n\n");
            List<String> options = session.getAddingQuestionOptions();
            for (int i = 0; i < options.size(); i++) {
                msg.append(i + 1).append(". ").append(options.get(i)).append("\n");
            }
            msg.append("\n🔢 Введите номер правильного ответа (1-" + options.size() + "):");
            session.setAddingQuestionStep(3);
            send(chatId, msg.toString());
            return;
        }
        session.getAddingQuestionOptions().add(text.trim());
        send(chatId, "➕ Добавлен вариант: \"" + text + "\"\n" +
                "Отправьте ещё один или /done для завершения");
    }

    private void handleStepCorrectAnswer(long chatId, String text, UserSession session) {
        try {
            int answer = Integer.parseInt(text) - 1;
            List<String> options = session.getAddingQuestionOptions();
            if (answer < 0 || answer >= options.size()) {
                send(chatId, "❌ Нет такого варианта. Введите номер от 1 до " + options.size() + ":");
                return;
            }
            session.setAddingQuestionCorrectIndex(answer);
            session.setAddingQuestionStep(4);
            send(chatId, "✅ Правильный ответ: " + options.get(answer) +
                    "\n\n💰 Введите количество баллов за правильный ответ (число):");
        } catch (NumberFormatException e) {
            send(chatId, "🔢 Введите номер варианта цифрой:");
        }
    }

    private void handleStepPoints(long chatId, String text, UserSession session) {
        try {
            int points = Integer.parseInt(text);
            if (points < 0) {
                send(chatId, "❌ Баллы не могут быть отрицательными. Введите число >= 0:");
                return;
            }
            session.setAddingQuestionPoints(points);
            saveNewQuestion(session);
            session.setAddingQuestion(false);
            session.setAddingQuestionStep(0);
            send(chatId, "🎉 Вопрос успешно добавлен!\n" +
                    "Используйте /add_question чтобы добавить ещё или /start для теста");
        } catch (NumberFormatException e) {
            send(chatId, "🔢 Введите число баллов:");
        }
    }

    private void saveNewQuestion(UserSession session) {
        try {
            Question newQuestion = new Question(
                    session.getAddingQuestionText().toString(),
                    new ArrayList<>(session.getAddingQuestionOptions()),
                    session.getAddingQuestionCorrectIndex(),
                    session.getAddingQuestionPoints()
            );
            categoryDAO.addQuestionToCategory(
                    session.getAddingQuestionCategoryId(),
                    newQuestion
            );
            System.out.println("✅ Вопрос сохранён в БД: " + newQuestion.getContent());
        } catch (Exception e) {
            System.err.println("❌ Ошибка сохранения вопроса: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== РЕЗУЛЬТАТЫ ====================

    private void showUserResults(long chatId, UserSession session) {
        TelegramUser user = userDAO.findByChatId(chatId).orElse(null);
        if (user == null) {
            send(chatId, "❌ Пользователь не найден");
            return;
        }

        List<TestResult> results = resultDAO.findByUser(user, 10);
        if (results.isEmpty()) {
            send(chatId, "📭 У вас пока нет пройденных тестов.\nНачните с /start");
            return;
        }

        StringBuilder msg = new StringBuilder("📊 Ваши последние результаты:\n\n");
        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            msg.append(i + 1).append(". ").append(r.toFormattedString()).append("\n\n");
        }

        TestResultDAO.UserStats stats = resultDAO.getUserStats(user);
        msg.append("─────────────────\n")
                .append("📈 Общая статистика:\n")
                .append("   🎯 Попыток: ").append(stats.attempts).append("\n")
                .append("   ⭐ Лучший результат: ").append(stats.bestPoints).append(" баллов\n")
                .append("   📊 Средний: ").append(String.format("%.1f", stats.avgPoints)).append(" баллов");

        send(chatId, msg.toString());
    }

    // ==================== ОТПРАВКА СООБЩЕНИЙ ====================

    private void send(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}