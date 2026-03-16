package org.amjonota;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.MapView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.amjonota.auth.AuthService;

import javafx.scene.control.ScrollPane;
import java.io.IOException;
import java.sql.SQLException;

public class AndolonDetailsController {


    @FXML private MapView andolonMapView;
    @FXML private HBox mapHbox;
    @FXML private ScrollPane postDetailsScroll;

    @FXML
    private VBox bgBox;

    public void initialize() {

        andolonMapView.initialize();
        andolonMapView.setCenter(new Coordinate(23.7351, 90.4000));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);

        clip.widthProperty().bind(bgBox.widthProperty());
        clip.heightProperty().bind(bgBox.heightProperty());

        bgBox.setClip(clip);

        Rectangle clip2 = new Rectangle();
        clip2.setArcHeight(20);
        clip2.setArcWidth(20);


        clip2.widthProperty().bind(mapHbox.widthProperty());
        clip2.heightProperty().bind(mapHbox.heightProperty());

        mapHbox.setClip(clip2);

        Platform.runLater(() -> {
            postDetailsScroll.setVvalue(0);
            andolonMapView.prefHeightProperty().bind(andolonMapView.widthProperty());
        });

    }




















    @FXML
    public void navHome(MouseEvent e) {
        try {
            App.setRoot("dashboard");
        } catch (IOException ex) {
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navProfile(MouseEvent e) {
        try {
            App.setRoot("profile");
        } catch (IOException ex) {
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
        } catch (SQLException ex) {
            System.err.println("Could not clear remember token: " + ex.getMessage());
        }
        Session.clear();
        try {
            App.setRoot("login");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
