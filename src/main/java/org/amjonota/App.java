package org.amjonota;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.amjonota.auth.AuthService;
import org.amjonota.model.User;

import java.io.IOException;
import java.sql.SQLException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        String landPage = "index";
        String savedToken = Session.loadToken();

        if (savedToken != null) {
            try {
                AuthService authService = new AuthService();
                User user = authService.validateRememberToken(savedToken);
                if (user != null) {
                    Session.setCurrentUser(user);
                    landPage = "dashboard";
                }
                else {
                    Session.clearToken();
                }
            }
            catch (SQLException e) {
                System.err.println("Auto login failed: " + e.getMessage());
                Session.clearToken();
            }
        }

        scene = new Scene(loadFXML("add_andolon"), 1100, 825);
        stage.setScene(scene);
        stage.setTitle("AndolonDesk");
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        try {
            DatabaseManager.getInstance();
        }
        catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
        launch();
    }

}
