package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import org.amjonota.auth.AuthService;
import org.amjonota.auth.OAuthService;
import org.amjonota.model.User;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {
    @FXML private ImageView loginImg;
    @FXML private HBox imgContainer;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button googleButton;
    @FXML private Button facebookButton;

    private final AuthService authService = new AuthService();
    private final OAuthService oauthService = new OAuthService();

    @FXML
    public void initialize() {
        loginImg.fitWidthProperty().bind(
                imgContainer.widthProperty()
        );
    }

    @FXML
    public void loginBtn() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (!Utils.isNonEmpty(email) || !Utils.isNonEmpty(password)) {
            showAlert("Validation Error", "Email and password cannot be empty.");
            return;
        }
        if (!Utils.isValidPassword(password)) {
            showAlert("Validation Error", "Password must be at least 8 characters.");
            return;
        }

        loginButton.setDisable(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    User userOpt = authService.login(email, password);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            loginButton.setDisable(false);
                            if (userOpt != null) {
                                try {
                                    App.setRoot("dashboard");
                                }
                                catch (IOException e) {
                                    showAlert("Error", e.getMessage());
                                }
                            }

                            else {
                                showAlert("Login Failed", "Invalid email or password.");
                            }
                        }

                    });
                }
                catch (AuthService.AuthException | SQLException e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            loginButton.setDisable(false);
                            showAlert("Error", e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    @FXML
    public void loginBtnGoogle() {
        startOAuth(OAuthService.Provider.GOOGLE);
    }

    @FXML
    public void loginBtnFacebook() {
        startOAuth(OAuthService.Provider.FACEBOOK);
    }

    private void startOAuth(OAuthService.Provider provider) {
        googleButton.setDisable(true);
        facebookButton.setDisable(true);
        loginButton.setDisable(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    oauthService.startOAuthFlow(provider);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            googleButton.setDisable(false);
                            facebookButton.setDisable(false);
                            loginButton.setDisable(false);
                            try {
                                App.setRoot("dashboard");
                            }
                            catch (IOException e) {
                                showAlert("Error", e.getMessage());
                            }
                        }
                    });
                }
                catch (Exception e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            googleButton.setDisable(false);
                            facebookButton.setDisable(false);
                            loginButton.setDisable(false);
                            showAlert("Login Failed", e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    @FXML
    public void setSceneRegister() throws IOException {
        App.setRoot("register");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
