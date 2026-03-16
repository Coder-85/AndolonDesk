package org.amjonota;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Coordinate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;
import org.w3c.dom.Text;
import javafx.scene.control.Alert;

import org.amjonota.auth.AuthService;



import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;


public class AddAndolonController {

    private final AuthService authService = new AuthService();

    @FXML
    private MapView andolonMapView;

    @FXML private TextField andolonTitle;
    @FXML private TextArea andolonDescription;
    @FXML private DatePicker andolonDate;
    @FXML private ComboBox andolonCategory;
    @FXML private Button andolonImgSelector;
    @FXML private Button andolonSubmitBtn;
    @FXML private TextField addressInShort;

    @FXML
    private Label fileNameLabel;
    private File selectedFile;
    private String picNewName;

    public void initialize() {
        andolonCategory.getItems().add("Category 1");
        andolonCategory.getItems().add("Category 2");
        andolonCategory.getItems().add("Category 3");
        andolonCategory.getItems().add("Category 4");
        andolonMapView.initialize();
        andolonMapView.setCenter(new Coordinate(23.7351, 90.4000));
    }


    @FXML
    public void andolonSubmit() {
        String title = andolonTitle.getText();
        String description = andolonDescription.getText();

        if (!Utils.isNonEmpty(title)) { showAlert(Alert.AlertType.ERROR,  "Submission Error", "Title is required."); return; }
        if (!Utils.isNonEmpty(description)) { showAlert(Alert.AlertType.ERROR,  "Submission Error", "Description is required."); return; }


        String eventDate;
        if(andolonDate.getValue() == null){
            showAlert(Alert.AlertType.ERROR,  "Submission Error", "Event date is required.");
            return;
        }else{
            eventDate = andolonDate.getValue() != null ? andolonDate.getValue().toString() : null;
        }
        String category;
        if(andolonCategory.getValue() != null){
            category = andolonCategory.getValue().toString();
        }else{
            showAlert(Alert.AlertType.ERROR,  "Submission Error", "Category is required.");
            return;
        }

        if(selectedFile == null){
            showAlert(Alert.AlertType.ERROR, "Submission Error", "Image is required.");
            return;
        }

        String address = addressInShort.getText();
        if (!Utils.isNonEmpty(address)) { showAlert(Alert.AlertType.ERROR,  "Submission Error", "Address is required."); return; }

        andolonSubmitBtn.setDisable(true);
        picNewName = selectedFile.getName();

        try {

            String extension = "";
            String name = selectedFile.getName();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex > 0) {
                extension = name.substring(dotIndex);
            }

            picNewName = System.currentTimeMillis() + extension;

            Path uploadDir = Paths.get("uploads");
            Files.createDirectories(uploadDir);

            Path destination = uploadDir.resolve(picNewName);

            Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    authService.addAndolon(title, description, eventDate, category, picNewName, address);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            andolonSubmitBtn.setDisable(false);
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Great News! Your Andolon post has been shared.");
                            try {
                                App.setRoot("dashboard"); // it will actually redirect to andolon_details page with the andolon id;
                            }
                            catch (IOException e) {
                                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                            }
                        }
                    });
                }
                catch (AuthService.AuthException | SQLException e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            andolonSubmitBtn.setDisable(false);
                            showAlert(Alert.AlertType.ERROR, "Andolon Submission Failed", e.getMessage());
                        }
                    });
                }
            }
        }).start();

    }

    @FXML
    public void picSelect() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) fileNameLabel.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            fileNameLabel.setText(selectedFile.getName());
        }
    }

//    public File getSelectedFile() {
//        return selectedFile;
//    }

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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
