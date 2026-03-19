package org.amjonota;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
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

    private int postID;
    private boolean isIDSet = false;
    private boolean isSaved = false;
    private boolean isAttending = false;
    private Marker protestLocationMarker;
    private CoordinateLine protestAreaPolygon;
    private Coordinate protestCoordinates;
    private final List<Coordinate> polygonCoordinates = new ArrayList<Coordinate>();
    @FXML private MapView andolonMapView;
    @FXML private HBox mapHbox;
    @FXML private ScrollPane postDetailsScroll;

    @FXML private Label title;
    @FXML private Label category;
    @FXML private Label postTime;
    @FXML private Label description;
    @FXML private Label eventTime;
    @FXML private Label address;
    @FXML private Label hostName;
    @FXML private Label joiningPplCount;
    @FXML private Label savedPplCount;
    @FXML private Label totalViewCount;

    @FXML private Button attendBtn;
    @FXML private Button saveBtn;
    @FXML private SVGPath saveIcon;

    @FXML
    private VBox bgBox;

    public void setPostID(int id) throws SQLException {
        postID = id;
        isIDSet = true;
        loadPostData();
        if(isSaved){
            saveBtn.setText("Saved");
            saveIcon.setContent("M192 64C156.7 64 128 92.7 128 128L128 544C128 555.5 134.2 566.2 144.2 571.8C154.2 577.4 166.5 577.3 176.4 571.4L320 485.3L463.5 571.4C473.4 577.3 485.7 577.5 495.7 571.8C505.7 566.1 512 555.5 512 544L512 128C512 92.7 483.3 64 448 64L192 64z");
        }

        if(isAttending){
            attendBtn.setText("Attending");
        }
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
                hostName.setText(rs.getString("author_name"));
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
        String sql = "SELECT * FROM " + colName +" WHERE user_id = ? AND protest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, postID);
            try (ResultSet rs = stmt.executeQuery()) {
                if(rs.next()){
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
                    }
                    catch (NumberFormatException e) {}
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
                if (Boolean.TRUE.equals(newValue) && protestCoordinates != null) {
                    applyMarker(protestCoordinates );
                    protestCoordinates = null;
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
        }
        catch (NumberFormatException e) {}
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


    private void updateStat(int type , int addOrSubtract) throws SQLException {
        String colName;
        int value;
        Label countTxtField;
        Connection conn = DatabaseManager.getInstance().getConnection();
        if(type == 1){
            colName = "member_count";
            countTxtField = joiningPplCount;
        }else if(type == 2){
            colName = "bookmarked_count";
            countTxtField = savedPplCount;
        }else{
            colName = "views";
            countTxtField = totalViewCount;
        }

        String sql = "SELECT * FROM protests WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    value = rs.getInt(colName);
                }else{
                    return;
                }
            }
        }



        String sql2 = "UPDATE protests SET " + colName + " = ? WHERE id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql2)) {
            int lastValue;
            if((value + addOrSubtract) < 0){
                stmt.setInt(1, (0));
                lastValue = 0;
            }else {
                stmt.setInt(1, (value+addOrSubtract));
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
                isSaved = true;
                saveBtn.setText("Saved");
                saveIcon.setContent("M192 64C156.7 64 128 92.7 128 128L128 544C128 555.5 134.2 566.2 144.2 571.8C154.2 577.4 166.5 577.3 176.4 571.4L320 485.3L463.5 571.4C473.4 577.3 485.7 577.5 495.7 571.8C505.7 566.1 512 555.5 512 544L512 128C512 92.7 483.3 64 448 64L192 64z");
                updateStat(2, 1);
            }
        }else{
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
                isAttending = true;
                attendBtn.setText("Attending");
                updateStat(1, 1);
            }
        }else{
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
