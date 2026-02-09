module org.amjonota {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.amjonota to javafx.fxml;
    exports org.amjonota;
}
