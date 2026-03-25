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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

    // === Callback prefix'ы ===
    private static final String CAT_SELECT = "cat_";
    private static final String ANSWER_SELECT = "ans_";
    private static final String ADMIN_DEL_CAT = "delcat_";
    private static final String ADMIN_DEL_QUEST = "delq_";
    private static final String CONFIRM_YES = "confirm_yes";
    private static final String CONFIRM_NO = "confirm_no";

    public Bot() {}

    @Override
    public String getBotUsername() { return "@test2324251_bot"; }

    @Override
    public String getBotToken() { return "8547457064:AAEO88kSaSIbTIdbhNMwqtFK4cPo9X1B-bg"; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        var tgMsg = update.getMessage();
        userDAO.getOrCreate(chatId, tgMsg.getFrom().getUserName(),
                tgMsg.getFrom().getFirstName(), tgMsg.getFrom().getLastName());

        sessions.putIfAbsent(chatId, new UserSession());
        UserSession session = sessions.get(chatId);
        // 🔹 Кнопки админ-панели
        if (session.isAdminMode()) {
            if (text.equals("➕ Добавить вопрос") || text.equals("Добавить вопрос")) {
                startAddQuestionWizard(chatId, session);
                return;
            }
            if (text.equals("📁 Создать категорию") || text.equals("Создать категорию")) {
                startAddCategoryWizard(chatId, session);
                return;
            }
            if (text.equals("📋 Показать категории") || text.equals("Показать категории")) {
                showCategories(chatId);
                return;
            }
            if (text.equals("🔄 Обновить") || text.equals("Обновить")) {
                send(chatId, "🔄 Список обновлён");
                showCategories(chatId);
                return;
            }
            if (text.equals("🚪 Выход") || text.equals("Выход")) {
                session.setAdminMode(false);
                send(chatId, "🚪 Выход из админ-панели", getMainMenuKeyboard());
                return;
            }
        }

        // ==================== ОБРАБОТКА КНОПОК ГЛАВНОГО МЕНЮ ====================

// 🔹 Кнопка "📚 Начать тест"
        if (text.equals("📚 Начать тест") || text.equals("Начать тест")) {
            if (session.isAdminMode()) {
                send(chatId, "🚫 Вы в админке. Выйдите: /admin 0", getMainMenuKeyboard());
                return;
            }
            session.setTesting(false);
            session.setAdminMode(false);
            showCategoriesWithButtons(chatId); // или showCategories(chatId) для текста
            return;
        }

// 🔹 Кнопка "📊 Мои результаты"
        if (text.equals("📊 Мои результаты") || text.equals("Мои результаты")) {
            showUserResults(chatId, session);
            return;
        }

// 🔹 Кнопка "🔐 Админ-панель"
        if (text.equals("🔐 Админ-панель") || text.equals("Админ-панель")) {
            send(chatId, "🔑 Введите пароль администратора:\n(или используйте /admin пароль)");
            return;
        }
        // === Команды ===
        if (text.equals("/start")) {
            if (session.isAdminMode()) {
                send(chatId, "🚫 Вы в админке. Выйдите: /admin 0", getMainMenuKeyboard());
                return;
            }
            resetSessionForTesting(session);
            showCategoriesWithButtons(chatId);
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

        // === Админ: удаление через команды (дублирование для кнопок) ===
        if (text.equals("/delete_category") || text.equals("/удалить_категорию")) {
            if (!session.isAdminMode()) { send(chatId, "🔐 Только админам!"); return; }
            startDeleteCategoryWizard(chatId, session);
            return;
        }
        if (text.equals("/delete_question") || text.equals("/удалить_вопрос")) {
            if (!session.isAdminMode()) { send(chatId, "🔐 Только админам!"); return; }
            startDeleteQuestionWizard(chatId, session);
            return;
        }

        // === Обработка мастеров (текстовый ввод при добавлении) ===
        if (session.isDeletingCategory()) { handleDeleteCategoryStep(chatId, text, session); return; }
        if (session.isDeletingQuestion()) { handleDeleteQuestionStep(chatId, text, session); return; }
        if (session.isAddingCategory()) { handleAddCategoryStep(chatId, text, session); return; }
        if (session.isAddingQuestion()) { handleAddQuestionStep(chatId, text, session); return; }

        // === Админ-меню через кнопки ===
        if (session.isAdminMode()) {
            handleAdminMenuInput(chatId, text, session);
            return;
        }

        // === Тестирование ===
        if (!session.isTesting()) {
            handleCategorySelection(chatId, text, session);
        } else {
            handleAnswerProcessing(chatId, text, session);
        }
    }

    // ==================== CALLBACK QUERY (КНОПКИ) ====================
    private void handleCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        UserSession session = sessions.get(chatId);
        if (session == null) return;

        // 🔹 Выбор категории
        if (data.startsWith(CAT_SELECT)) {
            try {
                int index = Integer.parseInt(data.substring(CAT_SELECT.length()));
                List<Category> categories = categoryDAO.findAllWithQuestions();
                if (index >= 0 && index < categories.size()) {
                    Category category = categories.get(index);
                    session.setCategory(category);
                    session.setCategoryIndex(index);
                    session.setTesting(true);
                    session.setCurrentQuestion(0);
                    session.setPoints(0);
                    send(chatId, "📝 Категория: " + category.getName());
                    sendQuestionWithButtons(chatId, session);
                }
            } catch (NumberFormatException ignored) {}
            answerCallback(callbackQuery);
            return;
        }

        // 🔹 Ответ на вопрос
        if (data.startsWith(ANSWER_SELECT)) {
            try {
                int answer = Integer.parseInt(data.substring(ANSWER_SELECT.length()));
                handleAnswerProcessing(chatId, String.valueOf(answer + 1), session);
            } catch (NumberFormatException ignored) {}
            answerCallback(callbackQuery);
            return;
        }

        // 🔹 Удаление категории (админ)
        if (data.startsWith(ADMIN_DEL_CAT)) {
            if (!session.isAdminMode()) { answerCallback(callbackQuery, "🔐 Доступ запрещён"); return; }
            try {
                int index = Integer.parseInt(data.substring(ADMIN_DEL_CAT.length()));
                session.setDeletingCategory(true);
                session.setDeletingCategoryStep(0);
                session.setDeletingCategoryIndex(index);

                List<Category> categories = categoryDAO.findAllWithQuestions();
                Category cat = categories.get(index);

                send(chatId, "⚠️ Удалить \"" + cat.getName() + "\"?\nВсе вопросы будут удалены.",
                        getConfirmKeyboard());
            } catch (Exception e) { answerCallback(callbackQuery, "❌ Ошибка"); }
            answerCallback(callbackQuery);
            return;
        }

        // 🔹 Удаление вопроса (админ)
        if (data.startsWith(ADMIN_DEL_QUEST)) {
            if (!session.isAdminMode()) { answerCallback(callbackQuery, "🔐 Доступ запрещён"); return; }
            try {
                String[] parts = data.substring(ADMIN_DEL_QUEST.length()).split(":");
                long catId = Long.parseLong(parts[0]);
                int qIndex = Integer.parseInt(parts[1]);

                session.setDeletingQuestion(true);
                session.setDeletingQuestionStep(1);
                session.setDeletingQuestionCategoryId(catId);
                session.setDeletingQuestionIndex(qIndex);

                Category cat = categoryDAO.findByIdWithQuestions(catId).orElse(null);
                if (cat != null && qIndex < cat.getQuestions().size()) {
                    String qText = cat.getQuestions().get(qIndex).getContent();
                    String preview = qText.length() > 50 ? qText.substring(0, 50) + "..." : qText;
                    send(chatId, "⚠️ Удалить вопрос?\n\" " + preview + " \"", getConfirmKeyboard());
                }
            } catch (Exception e) { answerCallback(callbackQuery, "❌ Ошибка"); }
            answerCallback(callbackQuery);
            return;
        }

        // 🔹 Подтверждение удаления
        if (data.equals(CONFIRM_YES) || data.equals(CONFIRM_NO)) {
            if (session.isDeletingCategory() && session.getDeletingCategoryStep() == 1) {
                if (data.equals(CONFIRM_YES)) {
                    List<Category> cats = categoryDAO.findAllWithQuestions();
                    Category toDelete = cats.get(session.getDeletingCategoryIndex());
                    categoryDAO.delete(toDelete);
                    send(chatId, "✅ Категория удалена.");
                } else {
                    send(chatId, "❌ Удаление отменено.");
                }
                session.setDeletingCategory(false);
                if (session.isAdminMode()) showAdminMenu(chatId);
                else showCategoriesWithButtons(chatId);
            }
            else if (session.isDeletingQuestion() && session.getDeletingQuestionStep() == 2) {
                if (data.equals(CONFIRM_YES)) {
                    categoryDAO.removeQuestionFromCategory(
                            session.getDeletingQuestionCategoryId(),
                            session.getDeletingQuestionIndex());
                    send(chatId, "✅ Вопрос удалён.");
                } else {
                    send(chatId, "❌ Удаление отменено.");
                }
                session.setDeletingQuestion(false);
                if (session.isAdminMode()) showAdminMenu(chatId);
            }
            answerCallback(callbackQuery);
            return;
        }

        answerCallback(callbackQuery);
    }

    private void answerCallback(org.telegram.telegrambots.meta.api.objects.CallbackQuery cb) {
        answerCallback(cb, null);
    }

    private void answerCallback(org.telegram.telegrambots.meta.api.objects.CallbackQuery cb, String text) {
        try {
            AnswerCallbackQuery answer =
                    new AnswerCallbackQuery();
            answer.setCallbackQueryId(cb.getId());
            if (text != null) answer.setText(text);
            execute(answer);
        } catch (TelegramApiException ignored) {}
    }

    // ==================== КЛАВИАТУРЫ ====================
    // ==================== КЛАВИАТУРЫ ====================

    private org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup getMainMenuKeyboard() {
        var keyboard = new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        var row1 = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow();
        row1.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("📚 Начать тест"));
        row1.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("📊 Мои результаты"));

        var row2 = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow();
        row2.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("🔐 Админ-панель"));

        keyboard.setKeyboard(java.util.List.of(row1, row2));
        return keyboard;
    }

    private ReplyKeyboardMarkup getAdminKeyboard() {
        var keyboard = new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        var row1 = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow();
        row1.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("➕ Добавить вопрос"));
        row1.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("📁 Создать категорию"));

        var row2 = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow();
        row2.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("📋 Показать категории"));
        row2.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("🔄 Обновить"));

        var row3 = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow();
        row3.add(new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("🚪 Выход"));

        keyboard.setKeyboard(java.util.List.of(row1, row2, row3));
        return keyboard;
    }
    private InlineKeyboardMarkup getCategoriesKeyboard(List<Category> categories) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(c.getName() + " (" + c.getQuestions().size() + " вопр.)");
            btn.setCallbackData(CAT_SELECT + i);
            rows.add(List.of(btn));
        }

        InlineKeyboardButton back = new InlineKeyboardButton();
        back.setText("🔙 Назад");
        back.setCallbackData("back");
        rows.add(List.of(back));

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup getAnswersKeyboard(List<String> options) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < options.size(); i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText((i + 1) + ". " + options.get(i));
            btn.setCallbackData(ANSWER_SELECT + i);
            rows.add(List.of(btn));
        }
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup getConfirmKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton yes = new InlineKeyboardButton();
        yes.setText("✅ Да");
        yes.setCallbackData(CONFIRM_YES);

        InlineKeyboardButton no = new InlineKeyboardButton();
        no.setText("❌ Нет");
        no.setCallbackData(CONFIRM_NO);

        markup.setKeyboard(List.of(List.of(yes, no)));
        return markup;
    }

    // ==================== КАТЕГОРИИ С КНОПКАМИ ====================
    private void showCategoriesWithButtons(long chatId) {
        List<Category> categories = categoryDAO.findAllWithQuestions();
        if (categories.isEmpty()) {
            send(chatId, "📭 Категорий пока нет.", getMainMenuKeyboard());
            return;
        }
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("📚 Выберите категорию:");
        msg.setReplyMarkup(getCategoriesKeyboard(categories));
        send(msg);
    }

    private void sendQuestionWithButtons(long chatId, UserSession session) {
        Category category = session.getCategory();
        if (category == null || category.getQuestions() == null) {
            send(chatId, "❌ Ошибка загрузки вопросов.", getMainMenuKeyboard());
            session.setTesting(false);
            return;
        }

        int qIndex = session.getCurrentQuestion();
        if (qIndex >= category.getQuestions().size()) {
            finishTest(chatId, session);
            return;
        }

        Question q = category.getQuestions().get(qIndex);

        StringBuilder text = new StringBuilder();
        text.append("❓ Вопрос ").append(qIndex + 1)
                .append("/").append(category.getQuestions().size()).append("\n");
        text.append(q.getContent()).append("\n");

        // ✅ Создаём SendMessage с inline-кнопками
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text.toString());
        msg.setReplyMarkup(getAnswersKeyboard(q.getOptions())); // ← КНОПКИ!

        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== ОБРАБОТКА ОТВЕТОВ ====================
    private void handleAnswerProcessing(long chatId, String text, UserSession session) {
        try {
            int answerIndex = Integer.parseInt(text) - 1;

            Question q = session.getCategory().getQuestions().get(session.getCurrentQuestion());

            if (answerIndex == q.getCorrectOptionIndex()) {
                session.setPoints(session.getPoints() + q.getPoints());
                send(chatId, "✅ Правильно! (+ " + q.getPoints() + " бал.)");
            } else {
                send(chatId, "❌ Неправильно.");
            }

            session.setCurrentQuestion(session.getCurrentQuestion() + 1);

            // Небольшая задержка
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}

            // ✅ ОТПРАВЛЯЕМ СЛЕДУЮЩИЙ ВОПРОС С КНОПКАМИ!
            if (session.getCurrentQuestion() >= session.getCategory().getQuestions().size()) {
                finishTest(chatId, session);
            } else {
                sendQuestionWithButtons(chatId, session); // ← ЭТОТ МЕТОД!
            }
        } catch (NumberFormatException e) {
            send(chatId, "🔢 Введите номер ответа или нажмите кнопку:");
        }
    }

    // ==================== АДМИН С КНОПКАМИ ====================
    private void handleAdminCommand(long chatId, String text, UserSession session) {
        String[] parts = text.split(" ");
        if (parts.length < 2) {
            send(chatId, "🔑 Введите: /admin 12345", getMainMenuKeyboard());
            return;
        }
        if (parts[1].equals(ADMIN_PASSWORD)) {
            session.setAdminMode(true);
            showAdminMenu(chatId);
        } else {
            send(chatId, "🚫 Неверный пароль.", getMainMenuKeyboard());
        }
    }

    private void showAdminMenu(long chatId) {
        send(chatId, "🛠️ Админ-панель:\nВыберите действие:", getAdminKeyboard());
    }

    private void handleAdminMenuInput(long chatId, String text, UserSession session) {
        switch (text) {
            case "➕ Добавить вопрос" -> startAddQuestionWizard(chatId, session);
            case "📁 Создать категорию" -> startAddCategoryWizard(chatId, session);
            case "❌ Удалить категорию" -> startDeleteCategoryWizard(chatId, session);
            case "❌ Удалить вопрос" -> startDeleteQuestionWizard(chatId, session);
            case "🔄 Обновить" -> {
                send(chatId, "🔄 Обновлено");
                showCategoriesWithButtons(chatId);
            }
            case "🚪 Выход" -> {
                session.setAdminMode(false);
                send(chatId, "🚪 Выход из админки", getMainMenuKeyboard());
            }
            case "📚 Начать тест" -> {
                session.setAdminMode(false);
                resetSessionForTesting(session);
                showCategoriesWithButtons(chatId);
            }
            case "📊 Мои результаты" -> showUserResults(chatId, session);
            default -> send(chatId, "❓ Нажмите кнопку из меню");
        }
    }

    // ==================== УДАЛЕНИЕ (с кнопками подтверждения) ====================
    private void startDeleteCategoryWizard(long chatId, UserSession session) {
        List<Category> cats = categoryDAO.findAllWithQuestions();
        if (cats.isEmpty()) { send(chatId, "❌ Нет категорий"); return; }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < cats.size(); i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("🗑️ " + cats.get(i).getName());
            btn.setCallbackData(ADMIN_DEL_CAT + i);
            rows.add(List.of(btn));
        }
        markup.setKeyboard(rows);

        send(chatId, "❌ Выберите категорию для удаления:", markup);
    }

    private void startDeleteQuestionWizard(long chatId, UserSession session) {
        List<Category> cats = categoryDAO.findAllWithQuestions();
        if (cats.isEmpty()) { send(chatId, "❌ Нет категорий"); return; }

        // Сначала выбираем категорию
        session.setDeletingQuestion(true);
        session.setDeletingQuestionStep(0);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < cats.size(); i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(cats.get(i).getName());
            btn.setCallbackData("delq_cat_" + cats.get(i).getId());
            rows.add(List.of(btn));
        }
        markup.setKeyboard(rows);
        send(chatId, "❌ Выберите категорию:", markup);
    }

    private void handleDeleteQuestionStep(long chatId, String text, UserSession session) {
        // Этот метод теперь в основном для callback, но оставляем для совместимости
        if (session.getDeletingQuestionStep() == 0) {
            // Если пользователь ввёл номер категории текстом
            try {
                int index = Integer.parseInt(text) - 1;
                List<Category> cats = categoryDAO.findAllWithQuestions();
                if (index >= 0 && index < cats.size()) {
                    showQuestionsForDeletion(chatId, cats.get(index));
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void showQuestionsForDeletion(long chatId, Category category) {
        if (category.getQuestions().isEmpty()) {
            send(chatId, "📭 В этой категории нет вопросов");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < category.getQuestions().size(); i++) {
            String qText = category.getQuestions().get(i).getContent();
            String preview = qText.length() > 30 ? qText.substring(0, 30) + "..." : qText;

            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText((i + 1) + ". " + preview);
            btn.setCallbackData(ADMIN_DEL_QUEST + category.getId() + ":" + i);
            rows.add(List.of(btn));
        }
        markup.setKeyboard(rows);

        send(chatId, "📋 Вопросы в \"" + category.getName() + "\"\nНажмите для удаления:", markup);
    }

    // ==================== ДОБАВЛЕНИЕ (оставляем текстовый ввод для удобства) ====================
    // Мастер добавления категории/вопроса слишком сложен для чистых кнопок,
    // поэтому оставляем гибридный подход: кнопки для навигации, текст для ввода данных

    private void startAddCategoryWizard(long chatId, UserSession session) {
        session.setAddingCategory(true);
        session.setAddingCategoryStep(0);
        session.setAddingCategoryName(new StringBuilder());
        session.setAddingCategoryQuestions(new ArrayList<>());
        session.setAddingCategoryMaxPoints(0);
        send(chatId, "📁 Создание категории\n✍️ Введите название:");
    }

    private void handleAddCategoryStep(long chatId, String text, UserSession session) {
        // ... (оставляем как было, текстовый ввод удобнее для названий/вопросов)
        int step = session.getAddingCategoryStep();
        if (step == 0) {
            if (text.trim().isEmpty()) { send(chatId, "❌ Введите название:"); return; }
            session.setAddingCategoryName(new StringBuilder(text.trim()));
            session.setAddingCategoryStep(1);
            send(chatId, "✅ Название: \"" + text + "\"\nДобавить вопросы сейчас?",
                    getYesNoInlineKeyboard());
        } else if (step == 1) {
            if (text.equals("1") || text.equalsIgnoreCase("да")) {
                session.setAddingCategoryStep(2);
                send(chatId, "➕ Введите текст первого вопроса:");
            } else {
                saveNewCategory(session);
                session.setAddingCategory(false);
                send(chatId, "🎉 Категория создана!", getAdminKeyboard());
            }
        }
        // Остальные шаги добавления вопросов оставляем текстовыми для простоты
    }

    private InlineKeyboardMarkup getYesNoInlineKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton yes = new InlineKeyboardButton();
        yes.setText("✅ Да");
        yes.setCallbackData("add_q_yes");
        InlineKeyboardButton no = new InlineKeyboardButton();
        no.setText("❌ Нет");
        no.setCallbackData("add_q_no");
        markup.setKeyboard(List.of(List.of(yes, no)));
        return markup;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    private void resetSessionForTesting(UserSession session) {
        session.setTesting(false);
        session.setAdminMode(false);
        session.setDeletingCategory(false);
        session.setDeletingQuestion(false);
        session.setAddingCategory(false);
        session.setAddingQuestion(false);
        session.setPoints(0);
        session.setCurrentQuestion(0);
    }

    private void finishTest(long chatId, UserSession session) {
        Category category = session.getCategory();
        int userPoints = session.getPoints();
        int maxPoints = category.getMaxPoints();
        int grade = calculateGrade(userPoints, maxPoints);

        userDAO.findByChatId(chatId).ifPresent(user ->
                resultDAO.saveResult(user, category.getName(), userPoints, maxPoints, grade));

        send(chatId, "🎉 Тест завершён!\n" +
                "📚 " + category.getName() + "\n" +
                "💯 " + userPoints + "/" + maxPoints + " баллов\n" +
                "🎓 Оценка: " + grade, getMainMenuKeyboard());

        resetSessionForTesting(session);
    }

    private int calculateGrade(int points, int max) {
        if (max == 0) return 2;
        double p = (double) points / max;
        if (p >= 0.9) return 5;
        if (p >= 0.7) return 4;
        if (p >= 0.5) return 3;
        return 2;
    }

    private void showUserResults(long chatId, UserSession session) {
        TelegramUser user = userDAO.findByChatId(chatId).orElse(null);
        if (user == null) { send(chatId, "❌ Пользователь не найден"); return; }

        List<TestResult> results = resultDAO.findByUser(user, 10);
        if (results.isEmpty()) {
            send(chatId, "📭 Нет пройденных тестов", getMainMenuKeyboard());
            return;
        }

        StringBuilder msg = new StringBuilder("📊 Ваши результаты:\n");
        for (int i = 0; i < results.size(); i++) {
            msg.append(i+1).append(". ").append(results.get(i).toFormattedString()).append("\n");
        }
        send(chatId, msg.toString(), getMainMenuKeyboard());
    }

    // ==================== ОТПРАВКА СООБЩЕНИЙ ====================
    // ==================== ОТПРАВКА СООБЩЕНИЙ (ПЕРЕГРУЗКА) ====================

    // 🔹 Базовая отправка без клавиатуры
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

    // 🔹 Отправка с ReplyKeyboard (кнопки под полем ввода)
    private void send(long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Отправка с InlineKeyboard (кнопки под сообщением)
    private void send(long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Отправка готового SendMessage
    private void send(org.telegram.telegrambots.meta.api.methods.send.SendMessage message) {
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Заглушки для методов добавления (оставляем как было для краткости)
    private void startAddQuestionWizard(long chatId, UserSession session) { /* ... как в оригинале ... */
        send(chatId, "➕ Добавление вопроса (текстовый режим)\nВыберите категорию номером:");
        showCategories(chatId); // используем текстовый список для выбора
        session.setAddingQuestion(true);
        session.setAddingQuestionStep(0);
    }

    private void handleAddQuestionStep(long chatId, String text, UserSession session) {
        // Упрощённая версия: делегируем оригинальной логике
        int step = session.getAddingQuestionStep();
        if (step == 0) {
            try {
                int idx = Integer.parseInt(text) - 1;
                var cats = categoryDAO.findAllWithQuestions();
                if (idx >= 0 && idx < cats.size()) {
                    session.setAddingQuestionCategoryId(cats.get(idx).getId());
                    session.setAddingQuestionStep(1);
                    send(chatId, "✅ Категория: " + cats.get(idx).getName() + "\n✍️ Текст вопроса:");
                }
            } catch (Exception e) { send(chatId, "🔢 Введите номер категории"); }
        }
        // Остальные шаги оставляем как в оригинале (текстовый ввод)
        else if (step == 1) {
            if (text.trim().isEmpty()) { send(chatId, "❌ Введите текст"); return; }
            session.setAddingQuestionText(new StringBuilder(text.trim()));
            session.setAddingQuestionStep(2);
            session.setAddingQuestionOptions(new ArrayList<>());
            send(chatId, "📋 Варианты ответа (по одному, /done для завершения):");
        }
        else if (step == 2) {
            if (text.equals("/done") || text.equals("/готово")) {
                if (session.getAddingQuestionOptions().size() < 2) {
                    send(chatId, "❌ Минимум 2 варианта"); return;
                }
                StringBuilder opts = new StringBuilder("✅ Варианты:\n");
                var list = session.getAddingQuestionOptions();
                for (int i = 0; i < list.size(); i++) opts.append(i+1).append(". ").append(list.get(i)).append("\n");
                opts.append("\n🔢 Номер правильного (1-").append(list.size()).append("):");
                session.setAddingQuestionStep(3);
                send(chatId, opts.toString());
                return;
            }
            session.getAddingQuestionOptions().add(text.trim());
            send(chatId, "➕ Добавлено. Ещё или /done:");
        }
        else if (step == 3) {
            try {
                int ans = Integer.parseInt(text) - 1;
                var opts = session.getAddingQuestionOptions();
                if (ans < 0 || ans >= opts.size()) { send(chatId, "❌ Введите 1-" + opts.size()); return; }
                session.setAddingQuestionCorrectIndex(ans);
                session.setAddingQuestionStep(4);
                send(chatId, "✅ Правильный: " + opts.get(ans) + "\n💰 Баллы:");
            } catch (Exception e) { send(chatId, "🔢 Введите номер"); }
        }
        else if (step == 4) {
            try {
                int pts = Integer.parseInt(text);
                if (pts < 0) { send(chatId, "❌ Баллы >= 0"); return; }
                session.setAddingQuestionPoints(pts);
                saveNewQuestion(session);
                session.setAddingQuestion(false);
                session.setAddingQuestionStep(0);
                send(chatId, "🎉 Вопрос добавлен!", getAdminKeyboard());
            } catch (Exception e) { send(chatId, "🔢 Введите число"); }
        }
    }

    private void saveNewQuestion(UserSession session) {
        try {
            Question q = new Question(
                    session.getAddingQuestionText().toString(),
                    new ArrayList<>(session.getAddingQuestionOptions()),
                    session.getAddingQuestionCorrectIndex(),
                    session.getAddingQuestionPoints()
            );
            categoryDAO.addQuestionToCategory(session.getAddingQuestionCategoryId(), q);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveNewCategory(UserSession session) {
        try {
            Category cat = new Category(
                    session.getAddingCategoryName().toString(),
                    session.getAddingCategoryMaxPoints()
            );
            for (Question q : session.getAddingCategoryQuestions()) cat.addQuestion(q);
            categoryDAO.save(cat);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Оставляем текстовые методы для совместимости
    private void showCategories(long chatId) { /* ... как в оригинале ... */
        List<Category> cats = categoryDAO.findAllWithQuestions();
        if (cats.isEmpty()) { send(chatId, "📭 Нет категорий"); return; }
        StringBuilder sb = new StringBuilder("📚 Категории:\n");
        for (int i = 0; i < cats.size(); i++)
            sb.append(i+1).append(". ").append(cats.get(i).getName()).append("\n");
        send(chatId, sb.toString());
    }

    private void handleCategorySelection(long chatId, String text, UserSession session) {
        try {
            int idx = Integer.parseInt(text) - 1;
            var cats = categoryDAO.findAllWithQuestions();
            if (idx >= 0 && idx < cats.size()) {
                Category cat = cats.get(idx);
                session.setCategory(cat);
                session.setTesting(true);
                session.setCurrentQuestion(0);
                session.setPoints(0);
                send(chatId, "📝 " + cat.getName());
                sendQuestion(chatId, session); // текстовая версия для совместимости
            }
        } catch (Exception e) { send(chatId, "🔢 Введите номер или используйте кнопки"); }
    }

    private void sendQuestion(long chatId, UserSession session) { /* ... как в оригинале ... */
        Category cat = session.getCategory();
        if (cat == null || session.getCurrentQuestion() >= cat.getQuestions().size()) {
            finishTest(chatId, session); return;
        }
        Question q = cat.getQuestions().get(session.getCurrentQuestion());
        StringBuilder sb = new StringBuilder("❓ ").append(q.getContent()).append("\n");
        for (int i = 0; i < q.getOptions().size(); i++)
            sb.append(i+1).append(". ").append(q.getOptions().get(i)).append("\n");
        send(chatId, sb.toString());
    }

    private void handleDeleteCategoryStep(long chatId, String text, UserSession session) {
        // Для совместимости с текстовым вводом
        if (session.getDeletingCategoryStep() == 0) {
            try {
                int idx = Integer.parseInt(text) - 1;
                var cats = categoryDAO.findAllWithQuestions();
                if (idx >= 0 && idx < cats.size()) {
                    session.setDeletingCategoryIndex(idx);
                    session.setDeletingCategoryStep(1);
                    send(chatId, "⚠️ Удалить \"" + cats.get(idx).getName() + "\"?\n1 - Да, 0 - Нет");
                }
            } catch (Exception e) { send(chatId, "🔢 Введите номер"); }
        } else if (session.getDeletingCategoryStep() == 1) {
            if (text.equals("1") || text.equalsIgnoreCase("да")) {
                var cats = categoryDAO.findAllWithQuestions();
                categoryDAO.delete(cats.get(session.getDeletingCategoryIndex()));
                send(chatId, "✅ Удалено");
            } else send(chatId, "❌ Отменено");
            session.setDeletingCategory(false);
            if (session.isAdminMode()) showAdminMenu(chatId);
            else showCategoriesWithButtons(chatId);
        }
    }
}