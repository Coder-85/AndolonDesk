package org.amjonota.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.amjonota.DatabaseManager;
import org.amjonota.Session;
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

    public User register(String name, String email, String password, String dateOfBirth, String securityQuestion, String securityAnswer) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(name)) throw new AuthException("Name is required.");
        if (!Utils.isNonEmpty(email)) throw new AuthException("Email is required.");
        if (!Utils.isValidPassword(password)) throw new AuthException("Password must be at least 8 characters.");
        if (!Utils.isNonEmpty(securityQuestion)) throw new AuthException("Security question is required.");
        if (!Utils.isNonEmpty(securityAnswer)) throw new AuthException("Security answer is required.");

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String answerHash = BCrypt.withDefaults().hashToString(12, securityAnswer.toCharArray());
        String createdAt = LocalDateTime.now().format(DATETIME_FORMAT);

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "INSERT INTO users (name, email, password_hash, provider, date_of_birth, security_question, security_answer_hash, created_at) VALUES (?, ?, ?, 'local', ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, hash);
            stmt.setString(4, dateOfBirth);
            stmt.setString(5, securityQuestion);
            stmt.setString(6, answerHash);
            stmt.setString(7, createdAt);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new AuthException("An unexpected database error occured!");

                User user = new User(keys.getInt(1), name, email, hash, "local", null, dateOfBirth);
                user.setSecurityQuestion(securityQuestion);
                user.setSecurityAnswerHash(answerHash);
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

                return mapUser(rs, storedHash);
            }
        }
    }

    public void updateProfile(int userId, String name, String dateOfBirth, String securityQuestion, String securityAnswer, String newPassword) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(name)) throw new AuthException("Name is required.");
        Connection conn = DatabaseManager.getInstance().getConnection();
        String provider = null;
        try (PreparedStatement pStmt = conn.prepareStatement("SELECT provider FROM users WHERE id = ?")) {
            pStmt.setInt(1, userId);
            try (ResultSet rs = pStmt.executeQuery()) {
                if (rs.next()) {
                    provider = rs.getString("provider");
                }
            }
        }
        if (!Utils.isNonEmpty(provider)) throw new AuthException("User not found.");
        boolean isLocal = "local".equals(provider);
        if (isLocal) {
            if (!Utils.isNonEmpty(securityQuestion)) throw new AuthException("Security question is required.");
            if (!Utils.isNonEmpty(securityAnswer)) throw new AuthException("Security answer is required.");
        }
        if (Utils.isNonEmpty(newPassword) && !Utils.isValidPassword(newPassword)) {
            throw new AuthException("Password must be at least 8 characters.");
        }

        String answerHash = isLocal ? BCrypt.withDefaults().hashToString(12, securityAnswer.toCharArray()) : null;

        if (isLocal && Utils.isNonEmpty(newPassword)) {
            String passwordHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
            String sql = "UPDATE users SET name = ?, date_of_birth = ?, security_question = ?, security_answer_hash = ?, password_hash = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, dateOfBirth);
                stmt.setString(3, securityQuestion);
                stmt.setString(4, answerHash);
                stmt.setString(5, passwordHash);
                stmt.setInt(6, userId);
                stmt.executeUpdate();
            }
        }

        else if (isLocal) {
            String sql = "UPDATE users SET name = ?, date_of_birth = ?, security_question = ?, security_answer_hash = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, dateOfBirth);
                stmt.setString(3, securityQuestion);
                stmt.setString(4, answerHash);
                stmt.setInt(5, userId);
                stmt.executeUpdate();
            }
        } else {
            if (Utils.isNonEmpty(newPassword)) {
                throw new AuthException("Password change is only available for local accounts.");
            }
            String sql = "UPDATE users SET name = ?, date_of_birth = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, dateOfBirth);
                stmt.setInt(3, userId);
                stmt.executeUpdate();
            }
        }
    }

    public String getSecurityQuestion(String email) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(email)) throw new AuthException("Email is required.");

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT provider, security_question FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new AuthException("No account found with this email.");
                if (!"local".equals(rs.getString("provider"))) {
                    throw new AuthException("Forgot password is only available for local accounts.");
                }
                String question = rs.getString("security_question");
                if (!Utils.isNonEmpty(question)) {
                    throw new AuthException("Security question is not set for this account.");
                }
                return question;
            }
        }
    }

    public void resetPassword(String email, String answer, String newPassword) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(email)) throw new AuthException("Email is required.");
        if (!Utils.isNonEmpty(answer)) throw new AuthException("Security answer is required.");
        if (!Utils.isValidPassword(newPassword)) throw new AuthException("Password must be at least 8 characters.");

        validateSecurityAnswer(email, answer);

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT id FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new AuthException("No account found with this email.");

                String newPasswordHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
                try (PreparedStatement update = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
                    update.setString(1, newPasswordHash);
                    update.setInt(2, rs.getInt("id"));
                    update.executeUpdate();
                }
            }
        }
    }

    public void validateSecurityAnswer(String email, String answer) throws AuthException, SQLException {
        if (!Utils.isNonEmpty(email)) throw new AuthException("Email is required.");
        if (!Utils.isNonEmpty(answer)) throw new AuthException("Security answer is required.");

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT provider, security_answer_hash FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new AuthException("No account found with this email.");
                if (!"local".equals(rs.getString("provider"))) {
                    throw new AuthException("Forgot password is only available for local accounts.");
                }
                String storedAnswerHash = rs.getString("security_answer_hash");
                if (!Utils.isNonEmpty(storedAnswerHash)) {
                    throw new AuthException("Security answer is not set for this account.");
                }
                BCrypt.Result result = BCrypt.verifyer().verify(answer.toCharArray(), storedAnswerHash);
                if (!result.verified) throw new AuthException("Security answer is incorrect.");
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
                return mapUser(rs, null);
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

    public User getUserById(int userId) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs, rs.getString("password_hash"));
            }
        }
    }

    private User mapUser(ResultSet rs, String passwordHash) throws SQLException {
        User user = new User(rs.getInt("id"),rs.getString("name"),rs.getString("email"),passwordHash,rs.getString("provider"),rs.getString("provider_id"),rs.getString("date_of_birth"));

        user.setSecurityQuestion(rs.getString("security_question"));
        user.setSecurityAnswerHash(rs.getString("security_answer_hash"));
        user.setCreatedAt(rs.getString("created_at"));
        return user;
    }
}
