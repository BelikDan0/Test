package org.example.testing;

import org.example.testing.category.Category;

import java.util.ArrayList;
import java.util.List;

public class Tested {
    String name="Тестирование";
    List<Category>categories=new ArrayList<>();
    int totalPoints=getAllPoints();

    public int getAllPoints(){
        return 0;
    }

    public Category choesCategory(List<Category> categories){
        System.out.println("Выберите категорию вопросов");
        for (int i = 0; i < categories.size(); i++) {

        }
        return null;
    }
    public void startCategory(Category category){

    }
}
