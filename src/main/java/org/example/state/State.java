package org.example.state;

public enum State {
    NONE,

    // тест
    CHOOSING_CATEGORY,
    ANSWERING,

    // админ
    ADMIN_MENU,
    ADMIN_ADD_CATEGORY_NAME,
    ADMIN_ADD_QUESTION_TEXT,
    ADMIN_ADD_OPTIONS,
    ADMIN_ADD_CORRECT,
    ADMIN_ADD_POINTS
}
