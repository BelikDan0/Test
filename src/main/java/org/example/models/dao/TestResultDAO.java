package org.example.models.dao;

import org.example.db.HibernateUtil;
import org.example.models.TelegramUser;
import org.example.models.TestResult;
import org.hibernate.Session;
import java.util.List;

public class TestResultDAO extends BaseDao<TestResult> {

    public TestResultDAO() {
        super(TestResult.class);
    }

    // 🔹 Сохранить новый результат
    public void saveResult(TelegramUser user, String categoryName, int userPoints, int maxPoints, int grade) {
        TestResult result = new TestResult(user, categoryName, userPoints, maxPoints, grade);
        save(result);
    }

    // 🔹 Получить последние результаты пользователя (по умолчанию 10)
    public List<TestResult> findByUser(TelegramUser user, int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT r FROM TestResult r WHERE r.user = :user ORDER BY r.completedAt DESC",
                            TestResult.class)
                    .setParameter("user", user)
                    .setMaxResults(limit)
                    .list();
        }
    }

    // 🔹 Получить результаты по конкретной категории
    public List<TestResult> findByUserAndCategory(TelegramUser user, String categoryName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT r FROM TestResult r WHERE r.user = :user AND r.categoryName = :cat ORDER BY r.completedAt DESC",
                            TestResult.class)
                    .setParameter("user", user)
                    .setParameter("cat", categoryName)
                    .list();
        }
    }

    // 🔹 Получить лучшую попытку по категории
    public TestResult findBestResult(TelegramUser user, String categoryName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT r FROM TestResult r WHERE r.user = :user AND r.categoryName = :cat ORDER BY r.userPoints DESC",
                            TestResult.class)
                    .setParameter("user", user)
                    .setParameter("cat", categoryName)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    // 🔹 Статистика пользователя: среднее, лучшее, количество попыток
    public UserStats getUserStats(TelegramUser user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Object[] stats = session.createQuery(
                            "SELECT COUNT(r), AVG(r.userPoints), MAX(r.userPoints) FROM TestResult r WHERE r.user = :user",
                            Object[].class)
                    .setParameter("user", user)
                    .uniqueResult();

            return new UserStats(
                    stats[0] != null ? ((Number) stats[0]).longValue() : 0,
                    stats[1] != null ? ((Number) stats[1]).doubleValue() : 0,
                    stats[2] != null ? ((Number) stats[2]).intValue() : 0
            );
        }
    }

    // 🔹 Внутренний класс для статистики
    public static class UserStats {
        public long attempts;
        public double avgPoints;
        public int bestPoints;

        public UserStats(long attempts, double avgPoints, int bestPoints) {
            this.attempts = attempts;
            this.avgPoints = avgPoints;
            this.bestPoints = bestPoints;
        }
    }
}