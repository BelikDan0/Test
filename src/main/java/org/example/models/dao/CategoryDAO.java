package org.example.models.dao;

import org.example.db.HibernateUtil;
import org.example.models.Category;
import org.example.models.Question;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.Optional;

public class CategoryDAO extends BaseDao<Category> {
    public CategoryDAO() {
        super(Category.class);
    }

    public List<Category> findAllWithQuestions() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.questions",
                    Category.class).list();
        }
    }

    public Optional<Category> findByIdWithQuestions(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Category category = session.createQuery(
                            "SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.questions WHERE c.id = :id",
                            Category.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return Optional.ofNullable(category);
        }
    }

    public void addQuestionToCategory(Long categoryId, Question question) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Category category = session.createQuery(
                            "SELECT c FROM Category c LEFT JOIN FETCH c.questions WHERE c.id = :id",
                            Category.class)
                    .setParameter("id", categoryId)
                    .uniqueResult();
            if (category != null) {
                if (category.getQuestions() == null) {
                    category.setQuestions(new java.util.ArrayList<>());
                }
                category.getQuestions().add(question);
                // 🔹 ПЕРЕСЧИТЫВАЕМ maxPoints
                int totalPoints = 0;
                for (Question q : category.getQuestions()) {
                    totalPoints += q.getPoints();
                }
                category.setMaxPoints(totalPoints);
                session.merge(category);
                tx.commit();
                System.out.println("✅ Вопрос добавлен. Новые maxPoints: " + totalPoints);
            } else {
                tx.rollback();
                throw new RuntimeException("Category not found: " + categoryId);
            }
        }
    }

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
                // 🔹 ПЕРЕСЧИТЫВАЕМ maxPoints
                int totalPoints = 0;
                for (Question q : category.getQuestions()) {
                    totalPoints += q.getPoints();
                }
                category.setMaxPoints(totalPoints);
                session.merge(category);
                tx.commit();
                System.out.println("✅ Вопрос удален. Новые maxPoints: " + totalPoints);
            } else {
                tx.rollback();
                throw new RuntimeException("Category not found or invalid question index");
            }
        }
    }
}