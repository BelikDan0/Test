package org.example;

import org.example.administration.AdminPanel;
import org.example.bot.Bot;
import org.example.main_menu.Menu;
import org.example.testing.Tested;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main(String[] args) throws Exception {

        Tested tested = new Tested();

        AdminPanel adminPanel = new AdminPanel(tested);

        adminPanel.generateTemplateCategories();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        botsApi.registerBot(new Bot(tested));

        System.out.println("Bot started");

    }
}
