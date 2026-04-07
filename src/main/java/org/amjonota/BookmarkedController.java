package org.amjonota;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.Node;
import javafx.scene.control.OverrunStyle;
import javafx.scene.shape.SVGPath;
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
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.time.LocalDate;

public class BookmarkedController {
    @FXML private VBox bookmarkList;
    @FXML private SVGPath dmIcon;
    @FXML private ComboBox<String> sortByTimeCombo;
    @FXML private ComboBox<String> sortByCategoryCombo;
    @FXML private TextField searchField;
    private List<ProtestItem> allBookmarked = new ArrayList<ProtestItem>();


    @FXML
    public void initialize() {
        User user = Session.getCurrentUser();
        if (user == null) return;
        sortByTimeCombo.getItems().add("Default");
        sortByTimeCombo.getItems().add("Upcoming");
        sortByTimeCombo.getItems().add("Ongoing");
        sortByTimeCombo.getItems().add("Previous");

        sortByCategoryCombo.getItems().add("Default");
        sortByCategoryCombo.getItems().add("Human Chain");
        sortByCategoryCombo.getItems().add("General Strike");
        sortByCategoryCombo.getItems().add("Blockade");
        sortByCategoryCombo.getItems().add("Rally");
        sortByCategoryCombo.getItems().add("Sit-in Protest");
        sortByCategoryCombo.getItems().add("Siege Protest");
        sortByCategoryCombo.getItems().add("Non-cooperation Movement");
        sortByCategoryCombo.getItems().add("Peaceful Protest");
        sortByCategoryCombo.getItems().add("Hunger Strike");
        sortByTimeCombo.setValue("Default");
        sortByCategoryCombo.setValue("Default");

        try {
            allBookmarked = loadBookmarkedProtests(user.getId());
            renderBookmarkList(allBookmarked);
        }
        catch (SQLException e) {
            System.err.println("Failed to load bookmarks: " + e.getMessage());
        }
        bindFilterListeners();

        Platform.runLater(() -> {
            if(hasUnreadMessages()){
                dmIcon.setContent("M568.4 37.7C578.2 34.2 589 36.7 596.4 44C603.8 51.3 606.2 62.2 602.7 72L424.7 568.9C419.7 582.8 406.6 592 391.9 592C377.7 592 364.9 583.4 359.6 570.3L295.4 412.3C290.9 401.3 292.9 388.7 300.6 379.7L395.1 267.3C400.2 261.2 399.8 252.3 394.2 246.7C388.6 241.1 379.6 240.7 373.6 245.8L261.2 340.1C252.1 347.7 239.6 349.7 228.6 345.3L70.1 280.8C57 275.5 48.4 262.7 48.4 248.5C48.4 233.8 57.6 220.7 71.5 215.7L568.4 37.7z");
                dmIcon.setStyle("-fx-fill: red;");
            }
        });
    }

    private List<ProtestItem> loadBookmarkedProtests(int id) throws SQLException {
        List<ProtestItem> items = new ArrayList<ProtestItem>();
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT p.*, u.name AS author_name, (SELECT COUNT(*) FROM user_bookmarks ub2 WHERE ub2.protest_id = p.id) AS bookmarked_count FROM protests p INNER JOIN user_bookmarks b ON p.id = b.protest_id INNER JOIN users u ON u.id = p.author_id WHERE b.user_id = ? ORDER BY p.created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProtestItem item = new ProtestItem(rs.getString("author_name"), rs.getInt("author_id"), rs.getString("posted_date"), rs.getString("title"), rs.getString("event_date"), rs.getString("summary"), rs.getString("description"), rs.getString("category"), rs.getInt("member_count"), rs.getInt("bookmarked_count"), rs.getString("img_name"));
                    item.setId(rs.getInt("id"));
                    items.add(item);
                }
            }
        }

        return items;
    }

    private void bindFilterListeners() {
        sortByTimeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortByCategoryCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        if (allBookmarked == null) {
            return;
        }

        String searchText = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String timeFilter = sortByTimeCombo.getValue() == null ? "Default" : sortByTimeCombo.getValue();
        String categoryFilter = sortByCategoryCombo.getValue() == null ? "Default" : sortByCategoryCombo.getValue();
        LocalDate today = LocalDate.now();

        List<ProtestItem> filtered = new ArrayList<ProtestItem>();
        for (ProtestItem item : allBookmarked) {
            if (!matchesSearch(item, searchText)) {
                continue;
            }
            if (!"Default".equals(categoryFilter) && !categoryFilter.equals(item.getCategory())) {
                continue;
            }
            if (!matchesTimeFilter(item, timeFilter, today)) {
                continue;
            }
            filtered.add(item);
        }

        if (!"Default".equals(timeFilter)) {
            filtered.sort(Comparator.comparing(p -> dateParse(p.getEventDate())));
        }

        renderBookmarkList(filtered);
    }

    private boolean matchesSearch(ProtestItem item, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        return toLower(item.getTitle()).contains(searchText) || toLower(item.getSummary()).contains(searchText) || toLower(item.getAuthor()).contains(searchText);
    }

    private boolean matchesTimeFilter(ProtestItem item, String timeFilter, LocalDate today) {
        if ("Default".equals(timeFilter)) {
            return true;
        }
        LocalDate eventDate = dateParse(item.getEventDate());
        if (eventDate == null) {
            return false;
        }
        if ("Upcoming".equals(timeFilter)) {
            return eventDate.isAfter(today);
        }
        if ("Ongoing".equals(timeFilter)) {
            return eventDate.isEqual(today);
        }
        if ("Previous".equals(timeFilter)) {
            return eventDate.isBefore(today);
        }

        return true;
    }

    private LocalDate dateParse(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String toLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void renderBookmarkList(List<ProtestItem> items) {
        bookmarkList.getChildren().clear();
        for (ProtestItem item : items) {
            bookmarkList.getChildren().add(buildCard(item));
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
        summary.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        summary.boundsInParentProperty().addListener((obs, oldVal, newVal) -> {
            Node txtNode = summary.lookup(".text");
            if (txtNode != null && txtNode instanceof Text) {
                item.setSummary(((Text)txtNode).getText());
            }
        });
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
