package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.event.MapViewEvent;
import com.sothawo.mapjfx.event.MarkerEvent;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;



import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class AddAndolonController {

    private boolean isEditingAndolon = false;
    private int editingAndolonID = -1;
    private Coordinate loadedCenterCoord;
    private String oldPicName;

    @FXML
    private MapView andolonMapView;

    @FXML private TextField andolonTitle;
    @FXML private TextArea andolonDescription;
    @FXML private DatePicker andolonDate;
    @FXML private ComboBox andolonCategory;
    @FXML private Button andolonImgSelector;
    @FXML private Button andolonSubmitBtn;
    @FXML private TextField addressInShort;
    @FXML private Label fileNameLabel;
    @FXML private RadioButton selectCenterPointRadio;
    @FXML private RadioButton defineAreaRadio;

    @FXML private SVGPath dmIcon;

    private File selectedFile;
    private String picNewName;
    private Marker selectedLocationMarker;
    private final List<Coordinate> areaCoordinates = new ArrayList<Coordinate>();
    private final List<Marker> areaMarkers = new ArrayList<Marker>();
    private CoordinateLine areaPolygon;
    private String mapCoordinates;
    private static final double COORD_TOGGLE_DELTA = 0.0000001d;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AddAndolonController(int editingAndolonID){
        this.editingAndolonID = editingAndolonID;
        this.isEditingAndolon = true;
    }

    public AddAndolonController(){}

    public void initialize() throws SQLException {
        andolonDescription.setWrapText(true);
        andolonCategory.getItems().add("Human Chain");
        andolonCategory.getItems().add("General Strike");
        andolonCategory.getItems().add("Blockade");
        andolonCategory.getItems().add("Rally");
        andolonCategory.getItems().add("Sit-in Protest");
        andolonCategory.getItems().add("Siege Protest");
        andolonCategory.getItems().add("Non-cooperation Movement");
        andolonCategory.getItems().add("Peaceful Protest");
        andolonCategory.getItems().add("Hunger Strike");
        andolonMapView.initialize();

        if(isEditingAndolon) {
            loadAndolonData();
        }else{
            andolonMapView.setCenter(new Coordinate(23.7351, 90.4000));
        }

        selectedLocationMarker = Marker.createProvided(Marker.Provided.RED).setVisible(false);
        selectCenterPointRadio.setSelected(true);

        andolonMapView.addEventHandler(MapViewEvent.MAP_CLICKED, new EventHandler<MapViewEvent>() {
            @Override
            public void handle(MapViewEvent event) {
                event.consume();
                Coordinate clicked = event.getCoordinate();

                if (clicked == null) {
                    return;
                }

                if (defineAreaRadio.isSelected()) {
                    toggleAreaPoint(clicked);
                }

                else {
                    selectedLocationMarker.setPosition(clicked).setVisible(true);
                    andolonMapView.addMarker(selectedLocationMarker);
                    mapCoordinates = clicked.getLatitude() + "," + clicked.getLongitude();
                }
            }
        });

        andolonMapView.addEventHandler(MarkerEvent.MARKER_CLICKED, new EventHandler<MarkerEvent>() {
            @Override
            public void handle(MarkerEvent event) {
                event.consume();

                Marker markerToRemove = event.getMarker();

                if (markerToRemove == selectedLocationMarker)
                    return;

                andolonMapView.removeMarker(markerToRemove);
                areaCoordinates.remove(indexOfMarker(markerToRemove));

                redrawAreaPolygon();
            }
        });

        Platform.runLater(() -> {
            if(hasUnreadMessages()){
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
        });
    }

    private void loadAndolonData() throws SQLException {

        andolonSubmitBtn.getStyleClass().remove("btn-primary");
        andolonSubmitBtn.getStyleClass().add("btn-warning");
        andolonSubmitBtn.setText("Edit");

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM protests WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, editingAndolonID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    try {
                        App.setRoot("dashboard");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    return;
                }
                andolonTitle.setText(rs.getString("title"));
                andolonCategory.setValue(rs.getString("category"));
                andolonDescription.setText(rs.getString("description"));
                addressInShort.setText(rs.getString("address"));
                andolonDate.setValue(LocalDate.parse(rs.getString("event_date")));
                oldPicName = rs.getString("img_name");
                fileNameLabel.setText("Choose file only if you want to change existing one");

                String coords = rs.getString("map_coordinates");
                mapCoordinates = coords;
                if (coords != null && !coords.trim().isEmpty()) {
                    String[] parts = coords.split(",");
                    if (parts.length == 2) {
                        try {
                            double lat = Double.parseDouble(parts[0].trim());
                            double lng = Double.parseDouble(parts[1].trim());
                            loadedCenterCoord = new Coordinate(lat, lng);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
                andolonMapView.setCenter(loadedCenterCoord != null ? loadedCenterCoord : new Coordinate(23.7351, 90.4000));

                String sql2 = "SELECT coordinates FROM protest_polygons WHERE protest_id = ?";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
                    stmt2.setInt(1, editingAndolonID);
                    List<Coordinate> loadedCoords = new ArrayList<>();

                    try (ResultSet rs2 = stmt2.executeQuery()) {
                        while (rs2.next()) {
                            String raw = rs2.getString("coordinates");
                            if (raw == null || raw.trim().isEmpty()) continue;
                            String[] parts = raw.split(",");
                            if (parts.length != 2) continue;
                            try {
                                double lat = Double.parseDouble(parts[0].trim());
                                double lng = Double.parseDouble(parts[1].trim());
                                loadedCoords.add(new Coordinate(lat, lng));
                            } catch (NumberFormatException e) { }
                        }
                    }

                    andolonMapView.initializedProperty().addListener((obs, wasInit, isNowInit) -> {
                        if (isNowInit) {
                            Platform.runLater(() -> {
                                for (Coordinate coord : loadedCoords) {
                                    toggleAreaPoint(coord);
                                }
                                if (loadedCenterCoord != null) {
                                    selectedLocationMarker.setPosition(loadedCenterCoord).setVisible(true);
                                    andolonMapView.addMarker(selectedLocationMarker);
                                }
                            });
                        }
                    });

                    if (andolonMapView.getInitialized()) {
                        Platform.runLater(() -> {
                            for (Coordinate coord : loadedCoords) {
                                toggleAreaPoint(coord);
                            }
                            if (loadedCenterCoord != null) {
                                selectedLocationMarker.setPosition(loadedCenterCoord).setVisible(true);
                                andolonMapView.addMarker(selectedLocationMarker);
                            }
                        });
                    }
                }

            }


        }
    }

    @FXML
    public void andolonSubmit() throws IOException {

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

            if(!isEditingAndolon){
                if(selectedFile == null){
                    showAlert(Alert.AlertType.ERROR, "Submission Error", "Image is required.");
                    return;
                }
            }

        String address = addressInShort.getText();
        if (!Utils.isNonEmpty(address)) { showAlert(Alert.AlertType.ERROR,  "Submission Error", "Address is required."); return; }
        if (!Utils.isNonEmpty(mapCoordinates)) {
            showAlert(Alert.AlertType.ERROR, "Submission Error", "Please click on the map to select the centre location of the protest.");
            return;
        }
        if (areaCoordinates.size() < 3) {
            showAlert(Alert.AlertType.ERROR, "Submission Error", "Select at least three points to define the area of the protest.");
            return;
        }

            andolonSubmitBtn.setDisable(true);
            if(selectedFile != null){
                picNewName = selectedFile.getName();
            }else{
                picNewName = oldPicName;
            }

            try {
                if(selectedFile != null){
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
                    if(isEditingAndolon){
                        Path oldFile = uploadDir.resolve(oldPicName);
                        Files.deleteIfExists(oldFile);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        addAndolon(title, description, eventDate, category, picNewName, address);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                andolonSubmitBtn.setDisable(false);
                                if(!isEditingAndolon){
                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Great News! Your Andolon post has been shared.");
                                }else{
                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Hurrah! Your Andolon has been edited.");
                                    sendEditNotification();
                                }
                                try {
                                    App.setRoot("dashboard");
                                }
                                catch (IOException e) {
                                    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                                }
                            }
                        });
                    }
                    catch (SQLException e) {
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

    private void sendEditNotification() {
        new Thread(() -> {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();

                String sql = "SELECT DISTINCT user_id FROM (" +
                        "SELECT user_id FROM user_bookmarks WHERE protest_id = ? " +
                        "UNION " +
                        "SELECT user_id FROM attending_protests WHERE protest_id = ?" +
                        ") AS interested_users WHERE user_id != ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, editingAndolonID);
                    stmt.setInt(2, editingAndolonID);
                    stmt.setInt(3, Session.getCurrentUser().getId());

                    List<Integer> userIds = new ArrayList<>();
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            userIds.add(rs.getInt("user_id"));
                        }
                    }

                    if (!userIds.isEmpty()) {
                        String insertSql = "INSERT INTO notifications (from_id, to_id, main_txt, type, status, protest_id) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            for (int userId : userIds) {
                                insertStmt.setInt(1, Session.getCurrentUser().getId());
                                insertStmt.setInt(2, userId);
                                insertStmt.setString(3, "An andolon you are following has been updated. Click to see the updates.");
                                insertStmt.setString(4, "protest_edit");
                                insertStmt.setString(5, "unread");
                                insertStmt.setInt(6, editingAndolonID);
                                insertStmt.addBatch();
                            }
                            insertStmt.executeBatch();
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addAndolon(String title, String description, String eventDate, String category, String imgName, String address) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();

        String sql;
        if(!isEditingAndolon){
            sql = "INSERT INTO protests (author_id, posted_date, title, event_date, summary, description, category, img_name, map_coordinates, address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }else{
            sql = "UPDATE protests SET author_id = ?, posted_date = ?, title = ?, event_date = ?, summary = ?, description = ?, category = ?, img_name = ?, map_coordinates = ?, address = ?, created_at = ? WHERE id = ?";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setString(2, LocalDate.now().toString());
            stmt.setString(3, title);
            stmt.setString(4, eventDate);
            stmt.setString(5, description);
            stmt.setString(6, description);
            stmt.setString(7, category);
            stmt.setString(8, imgName);
            stmt.setString(9, mapCoordinates);
            stmt.setString(10, address);
            stmt.setString(11, LocalDateTime.now().format(DATETIME_FORMAT));
            if(isEditingAndolon){
                stmt.setInt(12, editingAndolonID);
            }
            stmt.executeUpdate();

            int protestId = -1;
            if(!isEditingAndolon){
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        protestId = keys.getInt(1);
                    }
                }
            }else{
                protestId = editingAndolonID;
            }

            if(isEditingAndolon){
                String query = "DELETE FROM protest_polygons WHERE protest_id = ?";
                try (PreparedStatement stmt2 = conn.prepareStatement(query)) {
                    stmt2.setInt(1, editingAndolonID);
                    stmt2.executeUpdate();
                }
            }

            if (protestId > 0 && !areaCoordinates.isEmpty()) {
                String polygonSql = "INSERT INTO protest_polygons (protest_id, coordinates) VALUES (?, ?)";
                try (PreparedStatement polygonStmt = conn.prepareStatement(polygonSql)) {
                    for (Coordinate c : areaCoordinates) {
                        polygonStmt.setInt(1, protestId);
                        polygonStmt.setString(2, c.getLatitude() + "," + c.getLongitude());
                        polygonStmt.addBatch();
                    }
                    polygonStmt.executeBatch();
                }
            }
        }
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


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void toggleAreaPoint(Coordinate clicked) {
        Marker marker = Marker.createProvided(Marker.Provided.BLUE).setPosition(clicked).setVisible(true);
        areaMarkers.add(marker);
        areaCoordinates.add(clicked);
        andolonMapView.addMarker(marker);

        redrawAreaPolygon();
    }

    private int indexOfMarker(Marker marker) {
        Coordinate target = marker.getPosition();

        for (int i = 0; i < areaCoordinates.size(); i++) {
            Coordinate c = areaCoordinates.get(i);
            if (Math.abs(c.getLatitude() - target.getLatitude()) < COORD_TOGGLE_DELTA && Math.abs(c.getLongitude() - target.getLongitude()) < COORD_TOGGLE_DELTA) {
                return i;
            }
        }

        return -1;
    }

    private void redrawAreaPolygon() {
        if (areaPolygon != null) {
            andolonMapView.removeCoordinateLine(areaPolygon);
            areaPolygon = null;
        }
        if (areaCoordinates.size() >= 3) {
            areaPolygon = new CoordinateLine(areaCoordinates)
                .setClosed(true)
                .setColor(Color.DODGERBLUE)
                .setFillColor(Color.color(0.12, 0.45, 0.98, 0.2))
                .setWidth(2);

            andolonMapView.addCoordinateLine(areaPolygon);
            areaPolygon.setVisible(true);
        }
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
