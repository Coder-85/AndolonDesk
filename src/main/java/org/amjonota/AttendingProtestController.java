package org.amjonota;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;
import java.sql.SQLException;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.amjonota.model.ProtestItem;
import org.amjonota.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class AttendingProtestController {
    @FXML
    private VBox attendingList;

    @FXML
    public void initialize() {
        User user = Session.getCurrentUser();
        if (user == null) return;

        try {
            for (ProtestItem item : loadAttendingProtests(user.getId())) {
                attendingList.getChildren().add(buildCard(item));
            }
        }
        catch (SQLException e) {
            System.err.println("Failed to load bookmarks: " + e.getMessage());
        }
    }

    private List<ProtestItem> loadAttendingProtests(int id) throws SQLException {
        List<ProtestItem> items = new ArrayList<ProtestItem>();
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT p.*, u.name AS author_name, (SELECT COUNT(*) FROM attending_protests ub2 WHERE ub2.protest_id = p.id) AS attending_count FROM protests p INNER JOIN attending_protests b ON p.id = b.protest_id INNER JOIN users u ON u.id = p.author_id WHERE b.user_id = ? ORDER BY b.id DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProtestItem item = new ProtestItem(rs.getString("author_name"), rs.getInt("author_id"), rs.getString("posted_date"), rs.getString("title"), rs.getString("event_date"), rs.getString("summary"), rs.getString("description"), rs.getString("category"), rs.getInt("member_count"), rs.getInt("attending_count"));
                    item.setId(rs.getInt("id"));
                    items.add(item);
                }
            }
        }

        return items;
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
        wrapper.getStyleClass().add("post-box-parent-bookmarked");
        wrapper.setMaxHeight(HBox.USE_PREF_SIZE);
        wrapper.setMinHeight(HBox.USE_PREF_SIZE);
        wrapper.setMaxWidth(800);
        wrapper.setPrefHeight(210);
        wrapper.setPrefWidth(200);
        wrapper.setPadding(new Insets(0, 0, 0, 5));

        return wrapper;
    }
}
