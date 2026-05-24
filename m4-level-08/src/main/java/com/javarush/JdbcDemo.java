package com.javarush;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class JdbcDemo {

    public static void main(String[] args) {
        // 1. Настройка HikariCP (Connection Pool)
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/sakila?useSSL=false&serverTimezone=UTC");
        config.setUsername("root");      // замените на свой логин
        config.setPassword("sakila");      // замените на свой пароль
        config.setMaximumPoolSize(5);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            // 2. Создаём таблицу (если не существует)
            createTableIfNotExists(dataSource);

            // 3. Пример PreparedStatement – вставка одного студента
            insertOneStudent(dataSource, "Ivanov", "ivan@mail.ru");

            // 4. Пример Batching – пакетная вставка 10 записей
            insertBatchStudents(dataSource);

            // 5. Пример транзакции с rollback (вставка двух, потом откат)
            transactionExampleWithRollback(dataSource);

            // 6. Выборка данных через PreparedStatement
            selectStudents(dataSource);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTableIfNotExists(HikariDataSource ds) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS students (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100)
                )
                """;
        try (Statement stmt = ds.getConnection().createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'students' is ready");
        }
    }

    // PreparedStatement – вставка одного
    private static void insertOneStudent(HikariDataSource ds, String name, String email) throws SQLException {
        String sql = "INSERT INTO students (name, email) VALUES (?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            int rows = pstmt.executeUpdate();
            System.out.println("Inserted 1 student: " + name + ", rows affected: " + rows);
        }
    }

    // Batching – пакетная вставка 10 записей
    private static void insertBatchStudents(HikariDataSource ds) throws SQLException {
        String sql = "INSERT INTO students (name, email) VALUES (?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);   // отключаем autocommit для батча
            for (int i = 1; i <= 10; i++) {
                pstmt.setString(1, "Student_" + i);
                pstmt.setString(2, "student" + i + "@example.com");
                pstmt.addBatch();
            }
            int[] counts = pstmt.executeBatch();
            conn.commit();
            System.out.println("Batch insert: " + counts.length + " rows added");
        } catch (SQLException e) {
            System.out.println("Batch insert error: " + e.getMessage());
            // при ошибке в батче транзакция уже откатится, но можно явно вызвать rollback
        }
    }

    // Транзакция с rollback – вставляем двух, затем выбрасываем ошибку и откатываем
    private static void transactionExampleWithRollback(HikariDataSource ds) throws SQLException {
        String sql = "INSERT INTO students (name, email) VALUES (?, ?)";
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "Petrov");
                pstmt.setString(2, "petrov@mail.ru");
                pstmt.executeUpdate();

                pstmt.setString(1, "Sidorov");
                pstmt.setString(2, "sidorov@mail.ru");
                pstmt.executeUpdate();

                // Имитируем критическую ошибку после двух вставок
                throw new SQLException("Forced error after inserting two students");
            } catch (SQLException e) {
                System.out.println("Transaction rolled back due to error: " + e.getMessage());
                conn.rollback();       // отменяем обе вставки
                return;                // выход без commit
            }
            // conn.commit() – сюда не попадём
        }
    }

    // Выборка через PreparedStatement
    private static void selectStudents(HikariDataSource ds) throws SQLException {
        String sql = "SELECT id, name, email FROM students WHERE name LIKE ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Student_%");
            ResultSet rs = pstmt.executeQuery();
            System.out.println("\nList of students (name starting with 'Student_'):");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                System.out.printf("  id=%d, name=%s, email=%s%n", id, name, email);
            }
        }
    }
}