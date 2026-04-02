package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.amjonota.auth.AuthService;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private DatePicker dobPicker;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField securityQuestionField;
    @FXML private TextField securityAnswerField;
    @FXML private Button registerButton;
    @FXML private Hyperlink login;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        dobPicker.setEditable(false);
    }

    @FXML
    public void registerBtn() {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String dob = dobPicker.getValue() != null ? dobPicker.getValue().toString() : null;
        String securityQuestion = securityQuestionField.getText();
        String securityAnswer = securityAnswerField.getText();

        if (!Utils.isNonEmpty(name)) { showAlert("Validation Error", "Name is required."); return; }
        if (!Utils.isNonEmpty(email)) { showAlert("Validation Error", "Email is required."); return; }
        if (!Utils.isValidPassword(password)) { showAlert("Validation Error", "Password must be at least 8 characters."); return; }
        if (!password.equals(confirm)) { showAlert("Validation Error", "Passwords do not match."); return; }
        if (!Utils.isNonEmpty(securityQuestion)) { showAlert("Validation Error", "Security question is required."); return; }
        if (!Utils.isNonEmpty(securityAnswer)) { showAlert("Validation Error", "Security answer is required."); return; }

        registerButton.setDisable(true);
        login.setDisable(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    authService.register(name, email, password, dob, securityQuestion, securityAnswer);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            registerButton.setDisable(false);
                            login.setDisable(false);
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created! Please log in.");
                            try {
                                App.setRoot("login");
                            }
                            catch (IOException e) {
                                showAlert("Error", e.getMessage());
                            }
                        }
                    });
                }
                catch (AuthService.AuthException | SQLException e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            registerButton.setDisable(false);
                            login.setDisable(false);
                            showAlert("Registration Failed", e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    @FXML
    public void setSceneLogin() throws IOException {
        App.setRoot("login");
    }

    private void showAlert(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
