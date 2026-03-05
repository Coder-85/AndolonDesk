package org.amjonota;

import org.amjonota.model.User;
import java.util.prefs.Preferences;

public class Session {
    private static final Preferences PREFS = Preferences.userNodeForPackage(Session.class);
    private static final String TOKEN_KEY = "andolondesk_remember_token";

    private static User currentUser;

    private Session() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }

    public static void saveToken(String token) {
        PREFS.put(TOKEN_KEY, token);
    }

    public static String loadToken() {
        return PREFS.get(TOKEN_KEY, null);
    }

    public static void clearToken() {
        PREFS.remove(TOKEN_KEY);
    }
}
