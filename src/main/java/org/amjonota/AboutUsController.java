package org.amjonota;

import java.io.IOException;

public class AboutUsController {




    public void gotoHome(){
        try {
            App.setRoot("index");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void gotoHomeFeature(){
        try {
            App.setRoot("index");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        IndexController.isFeatureBtnClicked = true;
    }



    public void setLoginPage() {
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRegisterPage() {
        try {
            App.setRoot("Register");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
