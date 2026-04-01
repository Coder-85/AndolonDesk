package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.amjonota.auth.AuthService;
import javafx.geometry.Insets;
import org.amjonota.model.ProtestItem;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChatListController {


    @FXML
    private VBox chatList;
    @FXML private SVGPath dmIcon;


    @FXML
    public void goToChat() {
        try {
            App.setRoot("chat_area");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        try {
            for (VBox item : loadAllChats()) {
                chatList.getChildren().add(item);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load protests: " + e.getMessage());
        }

        Platform.runLater(() -> {
            if(hasUnreadMessages()){
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
        });
    }


    private List<VBox> loadAllChats() throws SQLException {
        List<VBox> items = new ArrayList<VBox>();

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM chat_list WHERE from_user_id = " + Session.getCurrentUser().getId() + " OR to_user_id = " + Session.getCurrentUser().getId() + " ORDER BY time_ms DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name;
                String date;
                String lastMessage;
                int id;
                boolean isBold = false;

                if (rs.getInt("from_user_id") == Session.getCurrentUser().getId()) {
                    name = rs.getString("to_name");
                    id = rs.getInt("to_user_id");
                    date = rs.getString("time");
                    lastMessage = "You: " + rs.getString("msg");
                } else {
                    name = rs.getString("from_name");
                    date = rs.getString("time");
                    lastMessage = rs.getString("msg");
                    id = rs.getInt("from_user_id");
                    if(rs.getString("status").equals("unread")){
                        isBold = true;
                    }
                }

                items.add(buildChatItem(name, date, lastMessage, id, isBold));
            }
        }

        return items;
    }


    private VBox buildChatItem(String name, String date, String lastMessage, int id, boolean isBold) {

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: 700;");

        VBox leftBox = new VBox(nameLabel);
        leftBox.setAlignment(Pos.BOTTOM_LEFT);
        leftBox.setPadding(new Insets(0, 0, 0, 30));
        HBox.setHgrow(leftBox, Priority.ALWAYS);

        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-font-size: 13;");

        VBox rightBox = new VBox(dateLabel);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setPrefWidth(200);
        rightBox.setMaxWidth(200);
        rightBox.setMinWidth(200);
        rightBox.setPadding(new Insets(0, 20, 0, 0));

        HBox topRow = new HBox(leftBox, rightBox);
        topRow.setPrefHeight(100);

        Label messageLabel = new Label(lastMessage);
        messageLabel.setStyle("-fx-font-size: 13; ");
        if(isBold){
            messageLabel.setStyle("-fx-font-weight: 700;");
        }

        HBox bottomRow = new HBox(messageLabel);
        bottomRow.setPadding(new Insets(0, 0, 0, 30));
        bottomRow.setPrefHeight(100);

        VBox root = new VBox(topRow, bottomRow);
        root.setPrefHeight(70);
        root.setPrefWidth(600);
        root.setMaxWidth(900);

        root.getStyleClass().add("chat-list");

        root.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("chat_area.fxml"));
                Parent rootMain = loader.load();

                ChatAreaController controller = loader.getController();
                controller.setUserData(id, name);

                Stage stage = (Stage) root.getScene().getWindow();
                stage.getScene().setRoot(rootMain);

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });

        return root;
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
