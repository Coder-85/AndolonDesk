package org.amjonota;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class LoginController {

    @FXML
    private ImageView loginImg;
    @FXML
    private HBox imgContainer;

    @FXML
    public void initialize() {

        loginImg.fitWidthProperty().bind(
                imgContainer.widthProperty()
        );

    }

    @FXML
    public void loginBtn(){

    }

    @FXML
    public void loginBtnGoogle(){

    }

    @FXML
    public void loginBtnFacebook(){

    }

    @FXML
    public void setSceneRegister() throws IOException {
        App.setRoot("register");
    }
}
