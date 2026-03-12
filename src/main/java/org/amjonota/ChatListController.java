package org.amjonota;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import org.amjonota.auth.AuthService;

import java.io.IOException;
import java.sql.SQLException;

public class ChatListController {


    @FXML
    public void goToChat(){
        try {
            App.setRoot("chat_area");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }




    @FXML
    public void navHome(MouseEvent e) {

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
