package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.SVGPath;
import org.amjonota.auth.AuthService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SearchResultController {

    @FXML private SVGPath dmIcon;


    @FXML public void initialize(){
        Platform.runLater(() -> {
            if(hasUnreadMessages()){
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
        });
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
            App.setRoot("add_andolon");
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
    public void navDMList(MouseEvent e) {
        try {
            App.setRoot("chat_list");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navProfile(MouseEvent e) {
        try {
            App.setRoot("profile");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
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
