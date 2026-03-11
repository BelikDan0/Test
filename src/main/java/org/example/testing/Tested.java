package org.example.testing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.testing.category.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tested {
    String name="Тестирование";
    List<Category>categories=new ArrayList<>();
    int totalPoints=getAllPoints();
    Scanner scanner = new Scanner(System.in)
    public int getAllPoints(){
        return 0;
    }

    public Category choesCategory(List<Category> categories){
        System.out.println("Выберите категорию вопросов");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("Категория - "+categories.get(i).getName()+". Номер "+i+1);
        }
        int numberCategory= scanner.nextInt();
        return categories.get(numberCategory-1);
    }
    public void startCategory(Category category){
        
    }
}
