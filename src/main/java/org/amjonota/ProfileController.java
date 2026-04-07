package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.amjonota.model.ProtestItem;
import org.amjonota.model.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileController {
    @FXML
    private Label profileName;
    @FXML
    private Label profileEmail;
    @FXML
    private Label profileDob;
    @FXML
    private Label profileJoined;
    @FXML
    private VBox postList;

    @FXML
    private Label lastAttendingTitle;
    @FXML
    private Label lastAttendingDate;

    @FXML
    private SVGPath dmIcon;


    @FXML
    private Label attendingLabel;
    @FXML
    private Label upcomingLabel;
    @FXML
    private Label missedLabel;
    @FXML
    private Label activerateLabel;
    private StatData statData;

    @FXML
    public void initialize() throws SQLException {
        User user = Session.getCurrentUser();
        if (user == null) return;

        statData = new StatData();
        setStatDataTxt();

        setAttendingData();

        profileName.setText(user.getName());
        profileEmail.setText(user.getEmail());
        profileDob.setText(user.getDateOfBirth() != null ? user.getDateOfBirth() : "N/A");
        profileJoined.setText(user.getCreatedAt() != null ? user.getCreatedAt() : "N/A");

        try {
            for (ProtestItem item : loadAllProtests()) {
                postList.getChildren().add(buildCard(item));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load protests: " + e.getMessage());
        }

        Platform.runLater(() -> {
            if (hasUnreadMessages()) {
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
        });
    }

    private void setStatDataTxt() {
        attendingLabel.setText(String.valueOf(statData.getAttended()));
        upcomingLabel.setText(String.valueOf(statData.getUpcoming()));
        missedLabel.setText(String.valueOf(statData.getMissed()));
        int total = statData.getProtestNum();
        int rate = total == 0 ? 0 : (int) (((double) statData.getAttended() / (double) total) * 100);
        activerateLabel.setText(rate + "%");
    }

    private void setAttendingData() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT p.id AS protest_id, p.title, p.event_date FROM attending_protests ap "
                + "INNER JOIN protests p ON ap.protest_id = p.id "
                + "WHERE ap.user_id = ? ORDER BY p.event_date DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    setLatestData(rs.getString("title"), rs.getString("event_date"));
                } else {
                    setNoRecentAttendingData();
                }
            }
        }
    }

    private void setLatestData(String title, String eventDateStr) {
        lastAttendingTitle.setText(title);
        LocalDate eventDate = LocalDate.parse(eventDateStr);
        LocalDate today = LocalDate.now();

        if (eventDate.isAfter(today)) {
            lastAttendingDate.setStyle("-fx-text-fill: #6358DC;");
            lastAttendingDate.setText("Attending • " + eventDateStr);
        } else {
            lastAttendingDate.setStyle("-fx-text-fill: #28C76F;");
            lastAttendingDate.setText("Attended • " + eventDateStr);
        }
    }

    private void setNoRecentAttendingData() {
        lastAttendingTitle.setText("No attending/attended protest yet");
        lastAttendingDate.setStyle("-fx-text-fill: #6b7280;");
        lastAttendingDate.setText("Join a protest to see activity");
    }


    private List<ProtestItem> loadAllProtests() throws SQLException {
        List<ProtestItem> items = new ArrayList<ProtestItem>();

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT p.*, u.name AS author_name, (SELECT COUNT(*) FROM user_bookmarks ub WHERE ub.protest_id = p.id) AS bookmarked_count FROM protests p INNER JOIN users u ON u.id = p.author_id WHERE p.author_id = ?  ORDER BY p.created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ProtestItem item = new ProtestItem(rs.getString("author_name"), rs.getInt("author_id"), rs.getString("posted_date"), rs.getString("title"), rs.getString("event_date"), rs.getString("summary"), rs.getString("description"), rs.getString("category"), rs.getInt("member_count"), rs.getInt("bookmarked_count"), rs.getString("img_name"));
                item.setId(rs.getInt("id"));
                items.add(item);
            }
        }

        return items;
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

        HBox topRow = new HBox();

        VBox left = new VBox(author, postedDate);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().addAll("btn", "btn-warning");

        Button dltBtn = new Button("Delete");
        dltBtn.getStyleClass().addAll("btn", "btn-danger");

        editBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("add_andolon.fxml"));

                loader.setControllerFactory(param -> {
                    return new AddAndolonController(item.getId());
                });

                Parent rootMain = loader.load();

                Stage stage = (Stage) editBtn.getScene().getWindow();
                stage.getScene().setRoot(rootMain);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox right = new HBox(dltBtn, editBtn);
        right.setSpacing(10);
        right.setAlignment(Pos.TOP_RIGHT);
        HBox.setHgrow(right, Priority.ALWAYS);

        topRow.getChildren().addAll(left, right);

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

        VBox card = new VBox(topRow, title, eventDate, summary, viewBtn);
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

        dltBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm");
            alert.setHeaderText("Do you really want to delete the andolon post titled '" + item.getTitle() + "'");
            alert.setContentText("Are you sure?");

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    deleteAndolon(item.getId(), wrapper, item.getImgName());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        return wrapper;
    }

    private void deleteAndolon(int deletingId, HBox postCard, String oldPicName) throws SQLException {
        String[] queries = {
                "DELETE FROM attending_protests WHERE protest_id = ?",
                "DELETE FROM notifications WHERE protest_id = ?",
                "DELETE FROM protest_polygons WHERE protest_id = ?",
                "DELETE FROM protests WHERE id = ?",
                "DELETE FROM user_bookmarks WHERE protest_id = ?"
        };
        Connection conn = DatabaseManager.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);

            for (String sql : queries) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, deletingId);
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            postList.getChildren().remove(postCard);

            Path uploadDir = Paths.get("uploads");
            Path oldFile = uploadDir.resolve(oldPicName);
            Files.deleteIfExists(oldFile);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Your post was deleted successfully");


        } catch (SQLException e) {
            conn.rollback();
            showAlert(Alert.AlertType.ERROR, "Error", "Error occured while trying to delete your post. Please Try again later.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    public void seeAllAttending() {
        try {
            App.setRoot("attending_protest");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void openEditProfilePage() {
        try {
            App.setRoot("edit_profile");
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navAddAndolon(MouseEvent e) {
        try {
            App.setRoot("add_andolon");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
    public void navDMList(MouseEvent e) {
        try {
            App.setRoot("chat_list");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navProfile(MouseEvent e) {

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
