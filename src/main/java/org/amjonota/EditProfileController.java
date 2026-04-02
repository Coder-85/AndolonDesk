package org.amjonota;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.application.Platform;
import org.amjonota.auth.AuthService;
import org.amjonota.model.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditProfileController {
    @FXML private SVGPath dmIcon;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField securityQuestionField;
    @FXML private TextField securityAnswerField;
    @FXML private VBox securityQuestionBox;
    @FXML private VBox securityAnswerBox;
    @FXML private VBox newPasswordBox;
    @FXML private VBox confirmPasswordBox;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label createdAtLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        User user = Session.getCurrentUser();

        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        emailField.setDisable(true);

        if (Utils.isNonEmpty(user.getDateOfBirth())) {
            dobPicker.setValue(java.time.LocalDate.parse(user.getDateOfBirth()));
        }

        boolean isLocal = "local".equals(user.getProvider());

        if (isLocal) {
            securityQuestionField.setText(user.getSecurityQuestion());
        }

        else {
            securityQuestionBox.setVisible(false);
            securityQuestionBox.setManaged(false);
            securityAnswerBox.setVisible(false);
            securityAnswerBox.setManaged(false);
            newPasswordBox.setVisible(false);
            newPasswordBox.setManaged(false);
            confirmPasswordBox.setVisible(false);
            confirmPasswordBox.setManaged(false);
            securityQuestionField.clear();
            securityAnswerField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        }

        createdAtLabel.setText(user.getCreatedAt() == null ? "N/A" : user.getCreatedAt());

        Platform.runLater(() -> {
            if (hasUnreadMessages()) {
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
        });
    }

    @FXML
    public void saveProfile() {
        User current = Session.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "No active user session found.");
            return;
        }

        String name = nameField.getText();
        String dob = dobPicker.getValue() == null ? null : dobPicker.getValue().toString();
        String email = emailField.getText();
        String securityQuestion = securityQuestionField.getText();
        String securityAnswer = securityAnswerField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        boolean isLocal = "local".equals(current.getProvider());

        if (!Utils.isNonEmpty(name)) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Name is required.");
            return;
        }
        if (!Utils.isNonEmpty(email)) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Email is required.");
            return;
        }
        if (!Utils.isNonEmpty(dob)) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Date of birth is required.");
            return;
        }
        if (isLocal) {
            if (!Utils.isNonEmpty(securityQuestion)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Security question is required.");
                return;
            }
            if (!Utils.isNonEmpty(securityAnswer)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Security answer is required.");
                return;
            }
        }

        if (Utils.isNonEmpty(newPassword) || Utils.isNonEmpty(confirmPassword)) {
            if (!Utils.isNonEmpty(newPassword) || !Utils.isNonEmpty(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Passwords do not match.");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Passwords do not match.");
                return;
            }
        }

        try {
            authService.updateProfile(current.getId(),name,dob, isLocal ? securityQuestion : null, isLocal ? securityAnswer : null, Utils.isNonEmpty(newPassword) ? newPassword : null);
            User refreshed = authService.getUserById(current.getId());
            if (refreshed != null) {
                Session.setCurrentUser(refreshed);
            }
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
            App.setRoot("profile");
        } catch (AuthService.AuthException | SQLException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", e.getMessage());
        }
    }

    @FXML
    public void cancelEdit() {
        try {
            App.setRoot("profile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean hasUnreadMessages() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT COUNT(*) AS unread_count FROM chat_list WHERE to_user_id = ? AND status = 'unread'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("unread_count") > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    public void navHome(MouseEvent e) {
        try { App.setRoot("dashboard"); } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    public void navAddAndolon(MouseEvent e) {
        try { App.setRoot("add_andolon"); } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    public void navBookmarked(MouseEvent e) {
        try { App.setRoot("bookmarked"); } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    public void navDMList(MouseEvent e) {
        try { App.setRoot("chat_list"); } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    public void navProfile(MouseEvent e) {
        try { App.setRoot("profile"); } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    public void navLogout(MouseEvent e) {
        try {
            String token = Session.loadToken();
            if (token != null) {
                new AuthService().deleteRememberToken(token);
                Session.clearToken();
            }
        } catch (SQLException ex) {
            System.err.println("Could not clear remember token: " + ex.getMessage());
        }
        Session.clear();
        try { App.setRoot("login"); } catch (IOException ex) { ex.printStackTrace(); }
    }
}
