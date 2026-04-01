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

            stmt.execute("CREATE TABLE IF NOT EXISTS protests (id INTEGER PRIMARY KEY AUTOINCREMENT, author_id INTEGER NOT NULL, posted_date DATE NOT NULL, title TEXT NOT NULL, event_date DATE NOT NULL, summary TEXT NOT NULL, description TEXT, category TEXT, member_count INTEGER NOT NULL DEFAULT 0, img_name TEXT, map_coordinates TEXT, views INTEGER NOT NULL DEFAULT 0, address TEXT, bookmarked_count integer default 0 not null, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, author_name TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_bookmarks (user_id INTEGER NOT NULL, protest_id INTEGER NOT NULL, PRIMARY KEY (user_id, protest_id), FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (protest_id) REFERENCES protests(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS remember_tokens (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, token TEXT NOT NULL UNIQUE, expires_at DATETIME NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS attending_protests (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, protest_id INTEGER NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (protest_id) REFERENCES protests(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS protest_polygons (protest_id INTEGER NOT NULL, coordinates TEXT NOT NULL, FOREIGN KEY (protest_id) REFERENCES protests(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS chat_list (id INTEGER PRIMARY KEY AUTOINCREMENT, from_user_id INTEGER, to_user_id INTEGER, from_name TEXT, to_name TEXT, msg TEXT, time_ms INTEGER, status TEXT, time DATETIME default CURRENT_TIMESTAMP )");

            stmt.execute("CREATE TABLE IF NOT EXISTS chat (id INTEGER PRIMARY KEY AUTOINCREMENT, from_id INTEGER, to_id INTEGER, from_name TEXT, to_name TEXT, msg TEXT, status TEXT, time DATETIME default CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, from_id INTEGER, to_id INTEGER, from_name TEXT, to_name TEXT, main_txt TEXT, type TEXT, status TEXT, time DATETIME default CURRENT_TIMESTAMP, protest_id INTEGER);");
        }
    }
}
