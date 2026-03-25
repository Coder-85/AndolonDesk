package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.amjonota.auth.AuthService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatAreaController {

    private int personUserID;
    private String personName;

    private ScheduledExecutorService scheduler;
    private int lastMessageId = 0;

    @FXML private Label name;
    @FXML private VBox allMsg;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea newMsgArea;

    @FXML
    public void initialize(){

    }


    @FXML
    public void goToChatList(){
        try {
            stopRealtimeChat();
            App.setRoot("chat_list");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setUserData(int id, String n){
        personUserID = id;
        personName = n;
        name.setText(personName);

        try {
            for (HBox item : loadAllChats()) {
                allMsg.getChildren().add(item);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load chats: " + e.getMessage());
        }
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
        });


        markLastChatListAsSeen();
        startRealtimeChat();
    }




    private List<HBox> loadAllChats() throws SQLException {
        List<HBox> items = new ArrayList<HBox>();

        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM chat WHERE (from_id = ? AND to_id = ?) OR (from_id = ? AND to_id = ?) ORDER BY id ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, personUserID);
            stmt.setInt(3, personUserID);
            stmt.setInt(4, Session.getCurrentUser().getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String msg = rs.getString("msg");
                String dateTime = rs.getString("time");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dateTimeFinal = LocalDateTime.parse(dateTime, formatter);
                String date = dateTimeFinal.toLocalDate().toString();
                String time = dateTimeFinal.toLocalTime().toString();

                int id = rs.getInt("id");
                if(rs.getInt("from_id") == Session.getCurrentUser().getId()){
                    items.add(buildSentMessage("You", date, time, msg));
                }else{
                    items.add(buildReceivedMessage(personName, date, time, msg));
                }
            }
        }

        return items;
    }



    @FXML
    public void sendNewMessage() throws SQLException {
        String messageText = newMsgArea.getText();
        if (messageText == null || messageText.trim().isEmpty()) return;
        newMsgArea.setText("");

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            String sql = "INSERT INTO chat (from_id, to_id, from_name, to_name, msg, status) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, personUserID);
                stmt.setString(3, Session.getCurrentUser().getName());
                stmt.setString(4, personName);
                stmt.setString(5, messageText);
                stmt.setString(6, "unread");
                stmt.executeUpdate();
            }

            String sqlDelete = "DELETE FROM chat_list WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, personUserID);
                stmt.setInt(3, personUserID);
                stmt.setInt(4, Session.getCurrentUser().getId());
                stmt.executeUpdate();
            }

            String sqlInsert = "INSERT INTO chat_list (from_user_id, to_user_id, from_name, to_name, msg, time_ms, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                stmt.setInt(1, Session.getCurrentUser().getId());
                stmt.setInt(2, personUserID);
                stmt.setString(3, Session.getCurrentUser().getName());
                stmt.setString(4, personName);
                stmt.setString(5, messageText);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.setString(7, "unread");
                stmt.executeUpdate();
            }

            refreshChat();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void refreshChat() {
        Platform.runLater(() -> {
            try {
                allMsg.getChildren().clear();

                for (HBox item : loadAllChats()) {
                    allMsg.getChildren().add(item);
                }

                Platform.runLater(() -> {
                    scrollPane.setVvalue(1.0);
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void startRealtimeChat() {

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (hasNewMessages()) {
                    refreshChat();
                    markLastChatListAsSeen();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private boolean hasNewMessages() throws SQLException {

        Connection conn = DatabaseManager.getInstance().getConnection();

        String sql = "SELECT MAX(id) AS max_id FROM chat WHERE (from_id = ? AND to_id = ?) OR (from_id = ? AND to_id = ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Session.getCurrentUser().getId());
            stmt.setInt(2, personUserID);
            stmt.setInt(3, personUserID);
            stmt.setInt(4, Session.getCurrentUser().getId());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int maxId = rs.getInt("max_id");

                if (maxId > lastMessageId) {
                    lastMessageId = maxId;
                    return true;
                }
            }
        }

        return false;
    }

    public void stopRealtimeChat() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }



    private void markLastChatListAsSeen() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            String sql = "SELECT id FROM chat_list WHERE from_user_id = ? AND to_user_id = ? AND status = 'unread' LIMIT 1";

            int lastId = -1;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, personUserID);
                stmt.setInt(2, Session.getCurrentUser().getId());

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    lastId = rs.getInt("id");
                }
            }

            if (lastId != -1) {
                String updateSql = "UPDATE chat_list SET status = 'seen' WHERE id = ?";

                try (PreparedStatement stmt2 = conn.prepareStatement(updateSql)) {
                    stmt2.setInt(1, lastId);
                    stmt2.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private HBox buildReceivedMessage(String name, String date, String time, String message) {

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 700;");
        nameLabel.getStyleClass().add("msg_name");

        HBox nameBox = new HBox(nameLabel);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("msg_time");

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("msg_time");

        VBox timeBox = new VBox(dateLabel, timeLabel);
        timeBox.setAlignment(Pos.CENTER_RIGHT);
        timeBox.setPrefWidth(120);
        timeBox.setMinWidth(120);
        timeBox.setMaxWidth(120);
        HBox.setMargin(timeBox, new Insets(0, 10, 0, 0));

        HBox topRow = new HBox(nameBox, timeBox);
        topRow.setPrefHeight(40);
        topRow.setStyle("-fx-border-color: #d3d3d3; -fx-border-width: 0 0 1 0;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("main_msg");

        HBox msgRow = new HBox(msgLabel);
        msgRow.setPadding(new Insets(0, 0, 10, 0));

        VBox messageBox = new VBox(topRow, msgRow);
        messageBox.setMaxWidth(650);
        messageBox.getStyleClass().add("received_msg");
        messageBox.setPadding(new Insets(0, 10, 0, 10));

        HBox root = new HBox(messageBox);
        root.setPrefWidth(200);
        root.setPadding(new Insets(0, 100, 0, 0));

        return root;
    }



    private HBox buildSentMessage(String name, String date, String time, String message) {

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 700;");
        nameLabel.getStyleClass().add("msg_name");

        HBox nameBox = new HBox(nameLabel);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("msg_time");

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("msg_time");

        VBox timeBox = new VBox(dateLabel, timeLabel);
        timeBox.setAlignment(Pos.CENTER_RIGHT);
        timeBox.setPrefWidth(120);
        timeBox.setMinWidth(120);
        timeBox.setMaxWidth(120);
        HBox.setMargin(timeBox, new Insets(0, 10, 0, 0));

        HBox topRow = new HBox(nameBox, timeBox);
        topRow.setPrefHeight(40);
        topRow.setStyle("-fx-border-color: #d3d3d3; -fx-border-width: 0 0 1 0;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-weight: 600;");
        msgLabel.getStyleClass().add("main_msg");

        HBox msgRow = new HBox(msgLabel);
        msgRow.setPadding(new Insets(0, 0, 10, 0));

        VBox messageBox = new VBox(topRow, msgRow);
        messageBox.setMaxWidth(650);
        messageBox.getStyleClass().add("sent_msg");
        messageBox.setPadding(new Insets(0, 10, 0, 10));

        HBox root = new HBox(messageBox);
        root.setAlignment(Pos.TOP_RIGHT);
        root.setPrefWidth(200);
        root.setPadding(new Insets(0, 0, 0, 100));

        return root;
    }


    @FXML
    public void navHome(MouseEvent e) {

    }

    @FXML
    public void navAddAndolon(MouseEvent e) {
        try {
            stopRealtimeChat();
            App.setRoot("add_andolon");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navBookmarked(MouseEvent e) {
        try {
            stopRealtimeChat();
            App.setRoot("bookmarked");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navProfile(MouseEvent e) {
        try {
            stopRealtimeChat();
            App.setRoot("profile");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void navLogout(MouseEvent e) {
        try {
            stopRealtimeChat();
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