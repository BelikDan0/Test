package org.example.models.dao;

import org.example.db.HibernateUtil;
import org.example.models.Category;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class CategoryDAO extends BaseDao<Category> {

    public CategoryDAO() {
        super(Category.class);
    }

    // 🔹 Получить все категории с вопросами (чтобы избежать LazyInitializationException)
    public List<Category> findAllWithQuestions() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT c FROM Category c LEFT JOIN FETCH c.questions",
                    Category.class).list();
        }
    }

    // 🔹 Найти категорию по ID с вопросами
    public Optional<Category> findByIdWithQuestions(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Category category = session.createQuery(
                            "SELECT c FROM Category c LEFT JOIN FETCH c.questions WHERE c.id = :id",
                            Category.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return Optional.ofNullable(category);
        }
    }

    // 🔹 Найти категорию по имени
    public Optional<Category> findByName(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Category category = session.createQuery(
                            "SELECT c FROM Category c WHERE c.name = :name",
                            Category.class)
                    .setParameter("name", name)
                    .uniqueResult();
            return Optional.ofNullable(category);
        }
    }

    // 🔹 Добавить вопрос в существующую категорию
    public void addQuestionToCategory(Long categoryId, Question question) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Category category = session.get(Category.class, categoryId);
            if (category != null) {
                category.getQuestions().add(question);
                session.merge(category);
            }

            tx.commit();
        }
    }

    // 🔹 Удалить вопрос из категории по индексам
    public void removeQuestionFromCategory(Long categoryId, int questionIndex) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Category category = session.createQuery(
                            "SELECT c FROM Category c LEFT JOIN FETCH c.questions WHERE c.id = :id",
                            Category.class)
                    .setParameter("id", categoryId)
                    .uniqueResult();

            if (category != null && questionIndex >= 0 && questionIndex < category.getQuestions().size()) {
                category.getQuestions().remove(questionIndex);
                session.merge(category);
            }

            tx.commit();
        }
    }

    // 🔹 Обновить вопрос в категории
    public void updateQuestionInCategory(Long categoryId, int questionIndex, Question updatedQuestion) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Category category = session.createQuery(
                            "SELECT c FROM Category c LEFT JOIN FETCH c.questions WHERE c.id = :id",
                            Category.class)
                    .setParameter("id", categoryId)
                    .uniqueResult();

            if (category != null && questionIndex >= 0 && questionIndex < category.getQuestions().size()) {
                // Обновляем поля существующего вопроса (чтобы сохранился ID)
                Question existing = category.getQuestions().get(questionIndex);
                existing.setContent(updatedQuestion.getContent());
                existing.setOptions(updatedQuestion.getOptions());
                existing.setCorrectOptionIndex(updatedQuestion.getCorrectOptionIndex());
                existing.setPoints(updatedQuestion.getPoints());
                session.merge(existing);
            }

            tx.commit();
        }
    }

    // 🔹 Подсчитать общее количество вопросов во всех категориях
    public long getTotalQuestionsCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT COUNT(q) FROM Question q", Long.class)
                    .uniqueResult();
        }
    }
}
