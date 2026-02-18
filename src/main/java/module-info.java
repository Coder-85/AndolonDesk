module org.amjonota {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.amjonota to javafx.fxml;
    exports org.amjonota;
}
