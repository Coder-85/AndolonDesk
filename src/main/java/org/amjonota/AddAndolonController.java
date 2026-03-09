package org.amjonota;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.amjonota.auth.AuthService;

import java.io.IOException;
import java.sql.SQLException;


public class AddAndolonController {

    @FXML private WebView andolonWebView;

    public void initialize(){
        andolonWebView.getEngine().load("https://www.google.com/maps/d/u/0/edit?mid=1rnh3aFJ6b0dz_pcHIfO8oBjqS2Tlu_o&usp=sharing");
    }


    @FXML
    public void andolonSubmit(){

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
