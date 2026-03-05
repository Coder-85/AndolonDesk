package org.amjonota;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.amjonota.auth.AuthService;
import java.sql.SQLException;

import org.amjonota.model.User;

import java.io.IOException;

public class ProfileController {
    @FXML private Label profileName;
    @FXML private Label profileEmail;
    @FXML private Label profileDob;
    @FXML private Label profileJoined;

    @FXML
    public void initialize() {
        User user = Session.getCurrentUser();
        if (user == null) return;

        profileName.setText(user.getName());
        profileEmail.setText(user.getEmail());
        profileDob.setText(user.getDateOfBirth() != null ? user.getDateOfBirth() : "N/A");
        profileJoined.setText(user.getCreatedAt() != null ? user.getCreatedAt() : "N/A");
    }

    @FXML
    public void navHome(MouseEvent e) {
        try {
            App.setRoot("dashboard");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navAddAndolon(MouseEvent e) {
        try {
            App.setRoot("dashboard");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navBookmarked(MouseEvent e) {
        try {
            App.setRoot("bookmarked");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navProfile(MouseEvent e) {
        
    }

    @FXML
    public void navLogout(MouseEvent e) {
        try {
            String token = Session.loadToken();
            if (token != null) {
                new AuthService().deleteRememberToken(token);
                Session.clearToken();
            }
        }
        catch (SQLException ex) {
            System.err.println("Could not clear remember token: " + ex.getMessage());
        }
        Session.clear();
        try {
            App.setRoot("login");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
