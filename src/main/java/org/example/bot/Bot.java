package org.example.bot;


import org.example.models.Category;
import org.example.models.TelegramUser;
import org.example.models.dao.CategoryDAO;
import org.example.models.dao.TelegramUserDao;
import org.example.testing.question.Question;
import org.example.user.UserSession;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final TelegramUserDao userDAO = new TelegramUserDao();
    private final Map<Long, UserSession> sessions = new HashMap<>();
    private final String ADMIN_PASSWORD = "12345";

    public Bot() {
        // Конструктор без аргументов
    }

    @Override
    public String getBotUsername() {
        return "@your_bot_name";
    }

    @Override
    public String getBotToken() {
        return "YOUR_BOT_TOKEN";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // 1. Регистрируем пользователя в БД
        var tgMsg = update.getMessage();
        userDAO.getOrCreate(
                chatId,
                tgMsg.getFrom().getUserName(),
                tgMsg.getFrom().getFirstName(),
                tgMsg.getFrom().getLastName()
        );

        sessions.putIfAbsent(chatId, new UserSession());
        UserSession session = sessions.get(chatId);

        if (text.equals("/start")) {
            showCategories(chatId);
            return;
        }

        if (text.startsWith("/admin")) {
            handleAdminCommand(chatId, text, session);
            return;
        }

        if (session.isAdminMode()) {
            handleAdminMenu(chatId, text, session);
            return;
        }

        if (!session.isTesting()) {
            handleCategorySelection(chatId, text, session);
        } else {
            handleAnswerProcessing(chatId, text, session);
        }
    }

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
        StringBuilder msg = new StringBuilder();
        msg.append("❓ Вопрос ").append(qIndex + 1).append("\n\n");
        msg.append(q.getContent()).append("\n\n");

        for (int i = 0; i < q.getOptions().size(); i++) {
            msg.append(i + 1).append(". ").append(q.getOptions().get(i)).append("\n");
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

        // Сохраняем результат в БД
        userDAO.updateUserScore(chatId, userPoints);

        send(chatId, "🎉 Тест завершён!\n\n" +
                "Категория: " + category.getName() +
                "\nБаллы: " + userPoints + " из " + maxPoints +
                "\nОценка: " + grade);

        session.setTesting(false);
        session.setPoints(0);
        session.setCurrentQuestion(0);

        // Небольшая задержка перед показом категорий
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
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

    private void handleAdminCommand(long chatId, String text, UserSession session) {
        String[] parts = text.split(" ");
        if (parts.length < 2) {
            send(chatId, "🔑 Введите пароль: /admin пароль");
            return;
        }
        if (parts[1].equals(ADMIN_PASSWORD)) {
            session.setAdminMode(true);
            send(chatId, "🛠️ Админ-панель:\n1 - Добавить категорию (в консоли)\n2 - Удалить категорию\n3 - Показать категории\n0 - Выход");
        } else {
            send(chatId, "🚫 Неверный пароль.");
        }
    }

    private void handleAdminMenu(long chatId, String text, UserSession session) {
        try {
            int cmd = Integer.parseInt(text);
            switch (cmd) {
                case 1 -> send(chatId, "➕ Добавление доступно через консоль приложения.");
                case 2 -> {
                    List<Category> cats = categoryDAO.findAllWithQuestions();
                    StringBuilder msg = new StringBuilder("Категории:\n");
                    for (int i = 0; i < cats.size(); i++) {
                        msg.append(i + 1).append(". ").append(cats.get(i).getName()).append("\n");
                    }
                    send(chatId, msg + "\nВведите номер для удаления (пока в консоли).");
                }
                case 3 -> showCategories(chatId);
                case 0 -> {
                    session.setAdminMode(false);
                    send(chatId, "🚪 Выход из админ-панели.");
                }
                default -> send(chatId, "❌ Нет такой команды.");
            }
        } catch (Exception e) {
            send(chatId, "🔢 Введите номер команды.");
        }
    }

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