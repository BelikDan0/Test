package org.example.testing;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.models.Category;
import org.example.models.dao.CategoryDAO;

import java.util.List;

@Data
@NoArgsConstructor
public class Tested {

    private String name = "Тестирование";

    // 🔹 Загружаем категории из БД при каждом обращении
    public List<Category> getCategories() {
        CategoryDAO dao = new CategoryDAO();
        return dao.findAllWithQuestions();
    }

    // 🔹 Получаем общую сумму баллов
    public int getAllPoints() {
        return getCategories().stream()
                .mapToInt(Category::getMaxPoints)
                .sum();
    }

    // 🔹 Выбор категории (возвращает из БД)
    public Category chooseCategory(int index) {
        List<Category> categories = getCategories();
        if (index < 0 || index >= categories.size()) {
            throw new IndexOutOfBoundsException("Нет такой категории");
        }
        return categories.get(index);
    }
}