package org.amjonota;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;

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

public class ProfileController {
    @FXML private Label profileName;
    @FXML private Label profileEmail;
    @FXML private Label profileDob;
    @FXML private Label profileJoined;
    @FXML private VBox postList;

    @FXML private Label lastAttendingTitle;
    @FXML private Label lastAttendingDate;

    @FXML private Label attendingLabel;
    @FXML private Label upcomingLabel;
    @FXML private Label missedLabel;
    @FXML private Label activerateLabel;
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
        }
        catch (SQLException e) {
            System.err.println("Failed to load protests: " + e.getMessage());
        }
    }

    private void setStatDataTxt(){
        attendingLabel.setText(String.valueOf(statData.getAttended()));
        upcomingLabel.setText(String.valueOf(statData.getUpcoming()));
        missedLabel.setText(String.valueOf(statData.getMissed()));
        activerateLabel.setText(String.valueOf((int)((double)statData.getAttended()/ (double)statData.getProtestNum()* 100) ) + "%");
    }

    private void setAttendingData() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM attending_protests WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int protestId = rs.getInt("protest_id");
                    setLatestData(protestId);
                }
            }
        }
    }

    private void setLatestData(int protestId) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM protests WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, protestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String eventDateStr = rs.getString("event_date");
                    LocalDate eventDate = LocalDate.parse(eventDateStr);
                    LocalDate today = LocalDate.now();
                    lastAttendingTitle.setText(title);

                    if (eventDate.isBefore(today) || eventDate.isEqual(today)) {
                        lastAttendingDate.setText("Attended • " + eventDateStr);
                    } else{
                        lastAttendingDate.setStyle("-fx-text-fill: #6358DC;");
                        lastAttendingDate.setText("Attending • " + eventDateStr);
                    }
                }
            }
        }
    }



    private List<ProtestItem> loadAllProtests() throws SQLException {
        List<ProtestItem> items = new ArrayList<ProtestItem>();

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT p.*, u.name AS author_name, (SELECT COUNT(*) FROM user_bookmarks ub WHERE ub.protest_id = p.id) AS bookmarked_count FROM protests p INNER JOIN users u ON u.id = p.author_id WHERE p.author_id = ?  ORDER BY p.posted_date DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ProtestItem item = new ProtestItem(rs.getString("author_name"), rs.getInt("author_id"),rs.getString("posted_date"), rs.getString("title"), rs.getString("event_date"), rs.getString("summary"), rs.getString("description"), rs.getString("category"), rs.getInt("member_count"), rs.getInt("bookmarked_count"));
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

        editBtn.setOnAction(e -> {
            try {


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        VBox right = new VBox(editBtn);
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

        return wrapper;
    }


    @FXML
    public void seeAllAttending() {
        try {
            App.setRoot("attending_protest");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
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
