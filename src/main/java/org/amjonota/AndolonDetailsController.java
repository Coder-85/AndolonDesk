package org.amjonota;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;

import javafx.scene.control.ScrollPane;
import org.amjonota.model.ProtestItem;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AndolonDetailsController {

    private int authorID;
    private String authorName;

    private int postID;
    private boolean isIDSet = false;
    private boolean isSaved = false;
    private boolean isAttending = false;
    private Marker protestLocationMarker;
    private CoordinateLine protestAreaPolygon;
    private Coordinate protestCoordinates;
    private final List<Coordinate> polygonCoordinates = new ArrayList<Coordinate>();
    @FXML
    private MapView andolonMapView;
    @FXML
    private HBox mapHbox;
    @FXML
    private ScrollPane postDetailsScroll;
    @FXML
    private ScrollPane outerScroll;

    @FXML private SVGPath dmIcon;

    @FXML
    private Label title;
    @FXML
    private Label category;
    @FXML
    private Label postTime;
    @FXML
    private Label description;
    @FXML
    private Label eventTime;
    @FXML
    private Label address;
    @FXML
    private Label hostName;
    @FXML
    private Label joiningPplCount;
    @FXML
    private Label savedPplCount;
    @FXML
    private Label totalViewCount;

    @FXML
    private Button attendBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private SVGPath saveIcon;
    @FXML
    private Button dmBtn;

    @FXML
    private VBox bgBox;



    public void setPostID(int id) throws SQLException {
        postID = id;
        isIDSet = true;
        loadPostData();


        if (isSaved) {
            saveBtn.setText("Saved");
            saveIcon.setContent("M192 64C156.7 64 128 92.7 128 128L128 544C128 555.5 134.2 566.2 144.2 571.8C154.2 577.4 166.5 577.3 176.4 571.4L320 485.3L463.5 571.4C473.4 577.3 485.7 577.5 495.7 571.8C505.7 566.1 512 555.5 512 544L512 128C512 92.7 483.3 64 448 64L192 64z");
        }

        if (isAttending) {
            attendBtn.setText("Attending");
        }

        scrollToTop(6);
    }

    private void loadPostData() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT p.*, u.name AS author_name, (SELECT COUNT(*) FROM user_bookmarks ub WHERE ub.protest_id = p.id) AS bookmarked_count FROM protests p INNER JOIN users u ON u.id = p.author_id WHERE p.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    try {
                        App.setRoot("dashboard");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    return;
                }
                title.setText(rs.getString("title"));
                category.setText(rs.getString("category"));
                postTime.setText("Posted: " + rs.getString("posted_date"));
                description.setText(rs.getString("description"));
                eventTime.setText(rs.getString("event_date"));
                address.setText(rs.getString("address"));

                authorName = rs.getString("author_name");
                authorID = rs.getInt("author_id");

                if (authorID == Session.getCurrentUser().getId()) {
                    dmBtn.setDisable(true);
                }

                hostName.setText(authorName);
                joiningPplCount.setText(String.valueOf(rs.getInt("member_count")));
                savedPplCount.setText(String.valueOf(rs.getInt("bookmarked_count")));
                totalViewCount.setText(String.valueOf(rs.getInt("views")));
                loadPolygonCoordinates();
                showProtestLocation(rs.getString("map_coordinates"));

                String imgPath = "uploads/" + rs.getString("img_name");

                File file = new File(imgPath);
                Image image = new Image(file.toURI().toString());
                BackgroundImage bgImage = new BackgroundImage(
                        image,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(
                                BackgroundSize.AUTO,
                                BackgroundSize.AUTO,
                                false,
                                false,
                                true,
                                true
                        )
                );

                bgBox.setBackground(new Background(bgImage));

                updateStat(3, 1);
                isSaved = checkIf("user_bookmarks");
                isAttending = checkIf("attending_protests");
            }


        }
    }

    private boolean checkIf(String colName) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM " + colName + " WHERE user_id = ? AND protest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, postID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadPolygonCoordinates() throws SQLException {
        polygonCoordinates.clear();
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT coordinates FROM protest_polygons WHERE protest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String raw = rs.getString("coordinates");
                    if (raw == null || raw.trim().isEmpty()) continue;
                    String[] parts = raw.split(",");
                    if (parts.length != 2) continue;
                    try {
                        double lat = Double.parseDouble(parts[0].trim());
                        double lng = Double.parseDouble(parts[1].trim());
                        polygonCoordinates.add(new Coordinate(lat, lng));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
    }

    public void initialize() {

        andolonMapView.initialize();
        andolonMapView.setCenter(new Coordinate(23.7351, 90.4000));
        protestLocationMarker = Marker.createProvided(Marker.Provided.RED).setVisible(false);
        andolonMapView.initializedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (Boolean.TRUE.equals(newValue)) {
                    if (protestCoordinates != null) {
                        applyMarker(protestCoordinates);
                        protestCoordinates = null;
                    }

                    scrollToTop(4);
                }
            }
        });

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
            if(hasUnreadMessages()){
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
            scrollToTop(4);
        });
    }

    private void scrollToTop(int remainingPulses) {
        if (remainingPulses <= 0) {
            return;
        }
        Platform.runLater(() -> {
            if (postDetailsScroll != null) {
                postDetailsScroll.setVvalue(0);
                postDetailsScroll.setHvalue(0);
            }

            scrollToTop(remainingPulses - 1);
        });
    }

    private void showProtestLocation(String mapCoordinates) {
        if (mapCoordinates == null || mapCoordinates.trim().isEmpty()) {
            return;
        }
        String[] parts = mapCoordinates.split(",");
        if (parts.length != 2) {
            return;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            Coordinate coordinate = new Coordinate(lat, lng);
            if (!andolonMapView.getInitialized()) {
                protestCoordinates = coordinate;
                return;
            }
            applyMarker(coordinate);
        } catch (NumberFormatException e) {
        }
    }

    private void applyMarker(Coordinate coordinate) {
        protestLocationMarker.setPosition(coordinate).setVisible(true);
        andolonMapView.addMarker(protestLocationMarker);
        andolonMapView.setCenter(coordinate);

        if (protestAreaPolygon != null) {
            andolonMapView.removeCoordinateLine(protestAreaPolygon);
            protestAreaPolygon = null;
        }

        protestAreaPolygon = new CoordinateLine(polygonCoordinates)
                .setClosed(true)
                .setColor(Color.DODGERBLUE)
                .setFillColor(Color.color(0.12, 0.45, 0.98, 0.2))
                .setWidth(2);
        andolonMapView.addCoordinateLine(protestAreaPolygon);
        protestAreaPolygon.setVisible(true);
    }


    private void updateStat(int type, int addOrSubtract) throws SQLException {
        String colName;
        int value;
        Label countTxtField;
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (type == 1) {
            colName = "member_count";
            countTxtField = joiningPplCount;
        } else if (type == 2) {
            String sql = "SELECT COUNT(*) AS count FROM user_bookmarks WHERE protest_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, postID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        savedPplCount.setText(String.valueOf(rs.getInt("count")));
                    }
                }
            }

            return;
        } else {
            colName = "views";
            countTxtField = totalViewCount;
        }

        String sql = "SELECT * FROM protests WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    value = rs.getInt(colName);
                } else {
                    return;
                }
            }
        }


        String sql2 = "UPDATE protests SET " + colName + " = ? WHERE id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql2)) {
            int lastValue;
            if ((value + addOrSubtract) < 0) {
                stmt.setInt(1, (0));
                lastValue = 0;
            } else {
                stmt.setInt(1, (value + addOrSubtract));
                lastValue = value + addOrSubtract;
            }

            stmt.setInt(2, postID);
            stmt.executeUpdate();

            countTxtField.setText(String.valueOf(lastValue));
        }

    }

    public void saveBtnAction() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (!isSaved) {
            String sql = "INSERT INTO user_bookmarks (user_id, protest_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, postID);
                stmt.executeUpdate();
                if(authorID != Session.getCurrentUser().getId()){
                    sendNotification("Someone has bookmarked your andolon titled '" + title.getText() + "'", "bookmarked");
                }
                isSaved = true;
                saveBtn.setText("Saved");
                saveIcon.setContent("M192 64C156.7 64 128 92.7 128 128L128 544C128 555.5 134.2 566.2 144.2 571.8C154.2 577.4 166.5 577.3 176.4 571.4L320 485.3L463.5 571.4C473.4 577.3 485.7 577.5 495.7 571.8C505.7 566.1 512 555.5 512 544L512 128C512 92.7 483.3 64 448 64L192 64z");
                updateStat(2, 1);
            }
        } else {
            String sql = "DELETE FROM user_bookmarks WHERE user_id = ? AND protest_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, postID);
                stmt.executeUpdate();
                isSaved = false;
                saveBtn.setText("Save");
                saveIcon.setContent("M128 128C128 92.7 156.7 64 192 64L448 64C483.3 64 512 92.7 512 128L512 545.1C512 570.7 483.5 585.9 462.2 571.7L320 476.8L177.8 571.7C156.5 585.9 128 570.6 128 545.1L128 128zM192 112C183.2 112 176 119.2 176 128L176 515.2L293.4 437C309.5 426.3 330.5 426.3 346.6 437L464 515.2L464 128C464 119.2 456.8 112 448 112L192 112z");
                updateStat(2, -1);
            }
        }
    }

    public void attendBtnAction() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (!isAttending) {
            String sql = "INSERT INTO attending_protests (user_id, protest_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, postID);
                stmt.executeUpdate();
                if(authorID != Session.getCurrentUser().getId()){
                    sendNotification("Hurrah! Your andolon titled '" + title.getText() + "' got one new attendee", "attendee");
                }
                isAttending = true;
                attendBtn.setText("Attending");
                updateStat(1, 1);
            }
        } else {
            String sql = "DELETE FROM attending_protests WHERE user_id = ? AND protest_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, postID);
                stmt.executeUpdate();
                isAttending = false;
                attendBtn.setText("Attend Protest");
                updateStat(1, -1);
            }
        }
    }

    @FXML
    public void dmHostBtn() throws SQLException {
        String sql = "INSERT INTO chat (from_id, to_id, msg, status) VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, authorID);
            stmt.setString(3, "Hello, I have something to know about your andolon '" + title.getText() + "'");
            stmt.setString(4, "unread");
            try {
                stmt.executeUpdate();
                if(authorID != Session.getCurrentUser().getId()){
                    sendNotification(Session.getCurrentUser().getName() + " wants to know something about your andolon titled '" + title.getText() + "'. Reply asap!", "dm");
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Message has been sent to host. Go to chat list for further conversation.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Error while trying to send message. Please try again later.");
            }

        }

        String sql2 = "SELECT * FROM chat_list WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql2)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, authorID);

            stmt.setInt(3, authorID);
            stmt.setInt(4, Session.getCurrentUser().getId());

            try (ResultSet rs = stmt.executeQuery()) {
                int rowID;
                if (rs.next()) {
                    rowID = rs.getInt("id");
                    String sqlLast = "DELETE FROM chat_list WHERE id = ?";
                    try (PreparedStatement stmt2 = conn.prepareStatement(sqlLast)) {
                        stmt2.setInt(1, rowID);
                        stmt2.executeUpdate();
                    }
                }
                String sqlLast = "INSERT INTO chat_list (from_user_id, to_user_id, msg, time_ms, status) values (?, ?, ?, ?, ?);";
                try (PreparedStatement stmt2 = conn.prepareStatement(sqlLast)) {
                    stmt2.setInt(1, Session.getCurrentUser().getId());
                    stmt2.setInt(2, authorID);
                    stmt2.setString(3, "Hello, I have something to know about your andolon '" + title.getText() + "'");
                    stmt2.setLong(4, System.currentTimeMillis());
                    stmt2.setString(5, "unread");
                    stmt2.executeUpdate();
                }

            }
        }
    }


    private void sendNotification(String mainText, String type) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "INSERT INTO notifications (from_id, to_id, main_txt, type, status, protest_id) VALUES(?, ?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, authorID);
            stmt.setString(3, mainText);
            stmt.setString(4, type);
            stmt.setString(5, "unread");
            stmt.setInt(6, postID);

            stmt.executeUpdate();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    private HBox buildCard(ProtestItem item) {
        Label author = new Label(item.getAuthor());
        author.setCursor(Cursor.HAND);
        author.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("user_profile.fxml"));
                Parent root = loader.load();

                UserProfileController controller = loader.getController();
                controller.setUserID(item.getAuthorID());

                Stage stage = (Stage) author.getScene().getWindow();
                stage.getScene().setRoot(root);

            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        author.getStyleClass().add("author");
        Label postedDate = new Label("Posted: " + item.getPostedDate());
        postedDate.getStyleClass().add("date");
        VBox.setMargin(postedDate, new Insets(0, 0, 10, 0));

        Label title = new Label(item.getTitle());
        title.getStyleClass().add("title");
        title.setWrapText(true);
        Label eventDate = new Label("Event Date: " + item.getEventDate());
        eventDate.getStyleClass().add("event");
        VBox.setMargin(eventDate, new Insets(0, 0, 10, 0));
        Label summary = new Label(item.getSummary());
        summary.setWrapText(true);
        VBox.setMargin(summary, new Insets(0, 0, 15, 0));
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().addAll("btn", "btn-primary");
        viewBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("andolon_details.fxml"));
                Parent root = loader.load();

                AndolonDetailsController controller = loader.getController();
                controller.setPostID(item.getId());

                Stage stage = (Stage) viewBtn.getScene().getWindow();
                stage.getScene().setRoot(root);

            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        viewBtn.setMnemonicParsing(false);

        VBox card = new VBox(author, postedDate, title, eventDate, summary, viewBtn);
        card.getStyleClass().add("post-card");
        card.setPadding(new Insets(10));
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        card.setMinHeight(VBox.USE_PREF_SIZE);
        card.setMinWidth(VBox.USE_PREF_SIZE);
        card.setPrefHeight(210);
        card.setPrefWidth(497);
        HBox.setHgrow(card, Priority.ALWAYS);
        HBox.setMargin(card, new Insets(0, 0, 20, 0));
        HBox wrapper = new HBox(card);
        wrapper.getStyleClass().add("post-box-parent");
        wrapper.setMaxHeight(HBox.USE_PREF_SIZE);
        wrapper.setMinHeight(HBox.USE_PREF_SIZE);
        wrapper.setMaxWidth(800);
        wrapper.setPrefHeight(210);
        wrapper.setPrefWidth(200);
        wrapper.setPadding(new Insets(0, 0, 0, 5));

        return wrapper;
    }
}
