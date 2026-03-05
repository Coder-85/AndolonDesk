package org.amjonota.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.amjonota.DatabaseManager;
import org.amjonota.Utils;
import org.amjonota.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuthService {
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    public User register(String name, String email, String password, String dateOfBirth) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(name)) throw new AuthException("Name is required.");
        if (!Utils.isNonEmpty(email)) throw new AuthException("Email is required.");
        if (!Utils.isValidPassword(password)) throw new AuthException("Password must be at least 8 characters.");

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String createdAt = LocalDateTime.now().format(DATETIME_FORMAT);

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "INSERT INTO users (name, email, password_hash, provider, date_of_birth, created_at) VALUES (?, ?, ?, 'local', ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, hash);
            stmt.setString(4, dateOfBirth);
            stmt.setString(5, createdAt);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new AuthException("An unexpected database error occured!");

                User user = new User(keys.getInt(1), name, email, hash, "local", null, dateOfBirth);
                user.setCreatedAt(createdAt);
                return user;
            }
        }
        catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                throw new AuthException("An account with this email already exists.");
            }
            throw e;
        }
    }

    public User login(String email, String password) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(email)) throw new AuthException("Email is required.");
        if (!Utils.isNonEmpty(password)) throw new AuthException("Password is required.");

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM users WHERE email = ? AND provider = 'local'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                String storedHash = rs.getString("password_hash");
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
                if (!result.verified) return null;

                User user = new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), storedHash, rs.getString("provider"), null, rs.getString("date_of_birth"));
                user.setCreatedAt(rs.getString("created_at"));

                return user;
            }
        }
    }

    public String generateRememberToken(int userId) throws SQLException {
        String ruuid = java.util.UUID.randomUUID().toString();
        String token = BCrypt.withDefaults().hashToString(4, ruuid.toCharArray()).replaceAll("[^A-Za-z0-9]", "");
        String expiresAt = LocalDateTime.now().plusDays(30).format(DATETIME_FORMAT);

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "INSERT INTO remember_tokens (user_id, token, expires_at) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, token);
            stmt.setString(3, expiresAt);
            stmt.executeUpdate();
        }

        return token;
    }

    public User validateRememberToken(String token) throws SQLException {
        if (token == null || token.isEmpty()) return null;

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT u.* FROM users u INNER JOIN remember_tokens rt ON u.id = rt.user_id WHERE rt.token = ? AND rt.expires_at > datetime('now')";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                User user = new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), null, rs.getString("provider"), rs.getString("provider_id"), rs.getString("date_of_birth"));
                user.setCreatedAt(rs.getString("created_at"));
                return user;
            }
        }
    }

    public void deleteRememberToken(String token) throws SQLException {
        if (token == null || token.isEmpty()) return;

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "DELETE FROM remember_tokens WHERE token = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        }
    }
}
