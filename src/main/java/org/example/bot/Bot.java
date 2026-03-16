package org.example.bot;

import org.example.administration.AdminPanel;
import org.example.testing.Tested;
import org.example.testing.category.Category;
import org.example.testing.question.Question;
import org.example.user.UserSession;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private Tested tested;

    private final String ADMIN_PASSWORD = "12345";

    private Map<Long, UserSession> sessions = new HashMap<>();

    public Bot(Tested tested) {
        this.tested = tested;
    }

    @Override
    public String getBotUsername() {
        return "@to_do_list_reminder_bot";
    }

    @Override
    public String getBotToken() {
        return "8586608522:AAGJoBLOl8mRuPZNcc7rNuZe_995ALFY5lo";
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        sessions.putIfAbsent(chatId, new UserSession());

        UserSession session = sessions.get(chatId);

        if (text.equals("/start")) {

            showCategories(chatId);
            return;

        }

        if (text.startsWith("/admin")) {

            String[] parts = text.split(" ");

            if (parts.length < 2) {
                send(chatId, "Введите пароль: /admin пароль");
                return;
            }

            if (parts[1].equals(ADMIN_PASSWORD)) {

                session.setAdminMode(true);

                send(chatId,
                        "Админ панель\n\n" +
                                "1 Добавить категорию\n" +
                                "2 Удалить категорию\n" +
                                "3 Редактировать вопрос\n" +
                                "4 Показать категории\n" +
                                "0 Выход");

            } else {

                send(chatId, "Неверный пароль");

            }

            return;
        }

        if (session.isAdminMode()) {

            adminMenu(chatId, text, session);
            return;

        }

        if (!session.isTesting()) {

            chooseCategory(chatId, text, session);

        } else {

            processAnswer(chatId, text, session);

        }

    }

    private void showCategories(long chatId) {

        StringBuilder msg = new StringBuilder("Выберите категорию:\n");

        List<Category> categories = tested.getCategories();

        for (int i = 0; i < categories.size(); i++) {

            msg.append(i + 1)
                    .append(" - ")
                    .append(categories.get(i).getName())
                    .append("\n");

        }

        send(chatId, msg.toString());
    }

    private void chooseCategory(long chatId, String text, UserSession session) {

        try {

            int index = Integer.parseInt(text) - 1;

            List<Category> categories = tested.getCategories();

            if (index < 0 || index >= categories.size()) {
                send(chatId, "Нет такой категории");
                return;
            }

            Category category = categories.get(index);

            session.setCategory(category);
            session.setCategoryIndex(index);
            session.setTesting(true);
            session.setCurrentQuestion(0);
            session.setPoints(0);

            send(chatId, "Категория: " + category.getName());

            sendQuestion(chatId, session);

        } catch (Exception e) {

            send(chatId, "Введите номер категории");

        }

    }

    private void sendQuestion(long chatId, UserSession session) {

        Question q = session.getCategory()
                .getQuestionList()
                .get(session.getCurrentQuestion());

        StringBuilder msg = new StringBuilder();

        msg.append("Вопрос ")
                .append(session.getCurrentQuestion() + 1)
                .append("\n\n");

        msg.append(q.getContent()).append("\n\n");

        for (int i = 0; i < q.getOptions().size(); i++) {

            msg.append(i + 1)
                    .append(". ")
                    .append(q.getOptions().get(i))
                    .append("\n");

        }

        send(chatId, msg.toString());

    }

    private void processAnswer(long chatId, String text, UserSession session) {

        try {

            int answer = Integer.parseInt(text) - 1;

            Question q = session.getCategory()
                    .getQuestionList()
                    .get(session.getCurrentQuestion());

            if (answer == q.getCorrectOptionIndex()) {

                session.setPoints(session.getPoints() + q.getPoints());

                send(chatId, "✅ Правильно");

            } else {

                send(chatId, "❌ Неправильно");

            }

            session.setCurrentQuestion(session.getCurrentQuestion() + 1);

            if (session.getCurrentQuestion()
                    >= session.getCategory().getQuestionList().size()) {

                finishTest(chatId, session);

            } else {

                sendQuestion(chatId, session);

            }

        } catch (Exception e) {

            send(chatId, "Введите номер ответа");

        }

    }

    private void finishTest(long chatId, UserSession session) {

        Category category = session.getCategory();

        int maxPoints = category.getMaxPoints();
        int userPoints = session.getPoints();

        int grade = calculateGrade(userPoints, maxPoints);

        send(chatId,
                "🎉 Категория завершена\n\n" +
                        "Категория: " + category.getName() +
                        "\nБаллы: " + userPoints + " из " + maxPoints +
                        "\nОценка: " + grade);

        session.setTesting(false);
        session.setPoints(0);
        session.setCurrentQuestion(0);

        showCategories(chatId);

    }

    private int calculateGrade(int userPoints, int maxPoints) {

        double percent = (double) userPoints / maxPoints;

        if (percent >= 0.9) return 5;
        if (percent >= 0.7) return 4;
        if (percent >= 0.5) return 3;

        return 2;
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

    private void adminMenu(long chatId, String text, UserSession session) {

        AdminPanel admin = new AdminPanel(tested);

        try {

            int command = Integer.parseInt(text);

            switch (command) {

                case 1:

                    send(chatId, "Добавление категории пока доступно только через консоль");
                    admin.addCategory();
                    break;

                case 2:

                    List<Category> categories = tested.getCategories();

                    StringBuilder msg = new StringBuilder("Категории:\n");

                    for (int i = 0; i < categories.size(); i++) {

                        msg.append(i + 1)
                                .append(" ")
                                .append(categories.get(i).getName())
                                .append("\n");

                    }

                    send(chatId, msg + "\nВведите номер для удаления");

                    break;

                case 3:

                    send(chatId, "Редактирование пока через консоль");
                    break;

                case 4:

                    showCategories(chatId);
                    break;

                case 0:

                    session.setAdminMode(false);
                    send(chatId, "Выход из админ панели");
                    break;

                default:

                    send(chatId, "Нет такой команды");

            }

        } catch (Exception e) {

            send(chatId, "Введите номер команды");

        }

    }

}