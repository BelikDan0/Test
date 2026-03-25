package org.example;

import org.example.administration.AdminPanel;
import org.example.bot.Bot;
import org.example.db.HibernateUtil;
import org.example.testing.Tested;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("🔌 Подключение к БД...");
            var sessionFactory = HibernateUtil.getSessionFactory();
            System.out.println("✅ База данных подключена.");

            Tested tested = new Tested();
            AdminPanel adminPanel = new AdminPanel(tested);

            // Создаём шаблоны, если БД пуста
            adminPanel.generateTemplateCategories();

            // Запуск бота
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new Bot());

            System.out.println("🤖 Бот запущен и ожидает сообщения...");

            // Корректное завершение работы
            Runtime.getRuntime().addShutdownHook(new Thread(HibernateUtil::shutdown));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}