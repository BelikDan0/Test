package org.example.testing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.administration.AdminPanel;
import org.example.testing.category.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tested {
    private String name="Тестирование";
    private List<Category>categories=new ArrayList<>();
    private int totalPoints=getAllPoints();
    int chiose=0;
    Scanner scanner = new Scanner(System.in);
    public int getAllPoints(){
        int points=0;
        for (int i = 0; i < categories.size(); i++) {
            points+=categories.get(i).getMaxPoints();
        }
        return points;
    }

    public Category choesCategory(){;
        System.out.println("Выберите категорию вопросов");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("Категория - "+categories.get(i).getName()+". Номер "+(i+1));
        }
        int numberCategory= scanner.nextInt();
        scanner.nextLine();
        return categories.get(numberCategory-1);
    }

    public void startCategory(){
        choesCategory().start();
    }
}
