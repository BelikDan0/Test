package org.example.testing.category;

import org.example.testing.question.Question;

import java.util.List;

public class Category {
    private String name;
    private Integer points;
    private List<Question> questionList;
    private Boolean isFinished;
    private Boolean isActive;

    public Category(String name, Integer points, List<Question> questionList, Boolean isFinished, Boolean isActive) {
        this.name = name;
        this.points = points;
        this.questionList = questionList;
        this.isFinished = isFinished;
        this.isActive = isActive;
    }

    public void showInfo() {
        System.out.println("Name:" + name +
                "Points: " + points +
                "isFinished: " + isFinished +
                "isActive: " + isActive +
                "QuestionList: ");

        for (int i = 0; i < questionList.size(); i++) {
            System.out.println(i + 1 + ". " + questionList.get(i));
        }
    }

    public void start() {
        if (questionList.isEmpty()) {
            System.out.println("Ошибка: в категории нет вопросов");
            return;
        }
        this.isActive = true;
        this.isFinished = false;
        System.out.println("Категория '" + name + "' начата. Всего вопросов:" + questionList.size());
    }



    public String getName () {
            return name;
    }

    public void setName (String name){
        this.name = name;
    }

    public Integer getPoints () {
        return points;
    }

    public void setPoints (Integer points){
        this.points = points;
    }

    public List<Question> getQuestionList () {
        return questionList;
    }

    public void setQuestionList (List < Question > questionList) {
        this.questionList = questionList;
    }

    public Boolean getFinished () {
        return isFinished;
    }

    public void setFinished (Boolean finished){
        isFinished = finished;
    }

    public Boolean getActive () {
        return isActive;
    }

    public void setActive (Boolean active){
        isActive = active;
    }
}