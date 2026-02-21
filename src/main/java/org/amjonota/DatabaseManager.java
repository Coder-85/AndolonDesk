package org.amjonota;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:AndolonDesk.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        initSchema();
    }

    public static DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, password_hash TEXT, provider TEXT NOT NULL DEFAULT 'local', provider_id TEXT, date_of_birth DATE, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS protests (id INTEGER PRIMARY KEY AUTOINCREMENT, author TEXT NOT NULL, posted_date DATE NOT NULL, title TEXT NOT NULL, event_date DATE NOT NULL, summary TEXT NOT NULL, description TEXT, category TEXT, member_count INTEGER NOT NULL DEFAULT 0, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_bookmarks (user_id INTEGER NOT NULL, protest_id INTEGER NOT NULL, PRIMARY KEY (user_id, protest_id), FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (protest_id) REFERENCES protests(id) ON DELETE CASCADE)");
        }
    }
}
