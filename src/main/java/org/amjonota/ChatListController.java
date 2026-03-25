package org.amjonota;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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


    @FXML
    public void navHome(MouseEvent e) {

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
