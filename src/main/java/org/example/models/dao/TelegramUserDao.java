package org.example.models.dao;

import org.example.db.HibernateUtil;
import org.example.models.TelegramUser;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class TelegramUserDao extends BaseDao<TelegramUser> {

    public TelegramUserDao() {
        super(TelegramUser.class);
    }

    // 🔹 Найти пользователя по chatId
    public Optional<TelegramUser> findByChatId(Long chatId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            TelegramUser user = session.createQuery(
                            "SELECT u FROM TelegramUser u WHERE u.chatId = :chatId",
                            TelegramUser.class)
                    .setParameter("chatId", chatId)
                    .uniqueResult();
            return Optional.ofNullable(user);
        }
    }

    // 🔹 Получить или создать пользователя (идемпотентная операция)
    public TelegramUser getOrCreate(Long chatId, String username, String firstName, String lastName) {
        return findByChatId(chatId).orElseGet(() -> {
            TelegramUser newUser = new TelegramUser(chatId, username, firstName, lastName);
            save(newUser);
            return newUser;
        });
    }
    public TelegramUser findByChatIdWithResults(Long chatId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT u FROM TelegramUser u " +
                                    "LEFT JOIN FETCH u.testResults r " +
                                    "WHERE u.chatId = :chatId",
                            TelegramUser.class)
                    .setParameter("chatId", chatId)
                    .uniqueResult();
        }
    }

    // 🔹 Обновить результат последнего теста
    public void updateUserScore(Long chatId, int score) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            TelegramUser user = session.createQuery(
                            "SELECT u FROM TelegramUser u WHERE u.chatId = :chatId",
                            TelegramUser.class)
                    .setParameter("chatId", chatId)
                    .uniqueResult();

            if (user != null) {
                user.setLastScore(score);
                session.merge(user);
            }

            tx.commit();
        }
    }

    // 🔹 Получить топ пользователей по результатам
    public List<TelegramUser> getTopUsers(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT u FROM TelegramUser u WHERE u.lastScore IS NOT NULL ORDER BY u.lastScore DESC",
                            TelegramUser.class)
                    .setMaxResults(limit)
                    .list();
        }
    }
}
