package org.example.user;

import lombok.Data;
import org.example.models.Category;
import org.example.models.Question;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserSession {

    // === Для тестирования ===
    private Category category;
    private int currentQuestion = 0;
    private int points = 0;
    private boolean testing = false;
    private int categoryIndex = -1;

    // === Для админ-режима ===
    private boolean adminMode = false;

    // === Для добавления вопроса в существующую категорию ===
    private boolean addingQuestion = false;
    private int addingQuestionStep = 0;
    private Long addingQuestionCategoryId;
    private StringBuilder addingQuestionText = new StringBuilder();
    private List<String> addingQuestionOptions = new ArrayList<>();
    private int addingQuestionCorrectIndex = -1;
    private int addingQuestionPoints = 0;

    // === Для добавления категории ===
    private boolean addingCategory = false;
    private int addingCategoryStep = 0;
    private StringBuilder addingCategoryName = new StringBuilder();
    private List<Question> addingCategoryQuestions = new ArrayList<>();
    private int addingCategoryMaxPoints = 0;

    // === Для добавления вопроса внутри создания категории ===
    private boolean addingQuestionInsideCategory = false;
    private int addingQuestionInsideStep = 0;
    private StringBuilder tempQuestionText = new StringBuilder();
    private List<String> tempQuestionOptions = new ArrayList<>();
    private int tempQuestionCorrectIndex = -1;
    private int tempQuestionPoints = 0;
}