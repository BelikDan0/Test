package org.example.user;

import lombok.Data;
import org.example.models.Category;
import org.example.models.Question;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserSession {
    // === Тестирование ===
    private Category category;
    private int currentQuestion = 0;
    private int points = 0;
    private boolean testing = false;
    private int categoryIndex = -1;

    // === Админ-режим ===
    private boolean adminMode = false;

    // === Добавление вопроса в существующую категорию ===
    private boolean addingQuestion = false;
    private int addingQuestionStep = 0;
    private Long addingQuestionCategoryId;
    private StringBuilder addingQuestionText = new StringBuilder();
    private List<String> addingQuestionOptions = new ArrayList<>();
    private int addingQuestionCorrectIndex = -1;
    private int addingQuestionPoints = 0;

    // === Добавление категории ===
    private boolean addingCategory = false;
    private int addingCategoryStep = 0;
    private StringBuilder addingCategoryName = new StringBuilder();
    private List<Question> addingCategoryQuestions = new ArrayList<>();
    private int addingCategoryMaxPoints = 0;

    // === Добавление вопроса внутри категории (wizard) ===
    private boolean addingQuestionInsideCategory = false;
    private int addingQuestionInsideStep = 0;
    private StringBuilder tempQuestionText = new StringBuilder();
    private List<String> tempQuestionOptions = new ArrayList<>();
    private int tempQuestionCorrectIndex = -1;
    private int tempQuestionPoints = 0;
    private boolean deletingCategory = false;
    private int deletingCategoryStep = 0; // 0 - выбор, 1 - подтверждение
    private int deletingCategoryIndex = -1;

    // === Удаление вопроса (дополнение) ===
    private int deletingQuestionIndex = -1;
    // === Удаление вопроса ===
    private boolean deletingQuestion = false;
    private int deletingQuestionStep = 0; // 0 - выбор категории, 1 - выбор вопроса
    private Long deletingQuestionCategoryId = null;


}