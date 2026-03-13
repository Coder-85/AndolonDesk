module org.amjonota {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.sothawo.mapjfx;

    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires java.prefs;
    requires jdk.httpserver;
    requires bcrypt;

    opens org.amjonota to javafx.fxml;
    exports org.amjonota;
    exports org.amjonota.model;
    exports org.amjonota.auth;
}
