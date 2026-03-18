package org.example.user;

import lombok.Data;
import org.example.models.Category;

@Data
public class UserSession {

    private Category category;
    private int currentQuestion = 0;
    private int points = 0;
    private boolean testing = false;
    private int categoryIndex = -1;
    private boolean adminMode = false;
}