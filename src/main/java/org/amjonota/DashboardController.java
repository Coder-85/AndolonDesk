package org.amjonota;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.amjonota.model.ProtestItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class DashboardController {
    @FXML private VBox feedList;

    @FXML
    public void initialize() {
        try {
            for (ProtestItem item : loadAllProtests()) {
                feedList.getChildren().add(buildCard(item));
            }
        }
        catch (SQLException e) {
            System.err.println("Failed to load protests: " + e.getMessage());
        }
    }

    private List<ProtestItem> loadAllProtests() throws SQLException {
        List<ProtestItem> items = new ArrayList<ProtestItem>();

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM protests ORDER BY posted_date DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ProtestItem item = new ProtestItem(rs.getString("author"), rs.getString("posted_date"), rs.getString("title"), rs.getString("event_date"), rs.getString("summary"), rs.getString("description"), rs.getString("category"), rs.getInt("member_count"));
                item.setId(rs.getInt("id"));
                items.add(item);
                System.out.println("Title: " + item.getDescription());
            }
        }

        return items;
    }

    @FXML
    public void navHome(MouseEvent e) {
        
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
        try {
             App.setRoot("profile");
        }
        catch (IOException ex) {
             ex.printStackTrace();
        }
    }

    @FXML
    public void navLogout(MouseEvent e) {
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
