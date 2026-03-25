package org.example.main_menu;

import org.example.administration.AdminPanel;
import org.example.testing.Tested;

import java.util.Scanner;

public class Menu {
    public Tested tested;
    public AdminPanel adminPanel;
    public Menu(Tested tested) {
        this.tested = tested;
        this.adminPanel = new AdminPanel(tested);
    }
    public void startProgram(){
        while (true){
            System.out.println("Выберите войти как админ(1) или тестируемый(2)");
            int menu=new Scanner(System.in).nextInt();
            if(menu<0||menu>2){
                System.out.println("Нет такого выбора");
                return;
            }
            switch (menu){
                case 1:
                    adminPanel.menuAdmin();
                    break;
                case 2:
                    tested.startCategory();
            }
        }
    }
}
