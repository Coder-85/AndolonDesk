package org.amjonota;

public class Utils {

    private Utils() {}

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isNonEmpty(String val) {
        return val != null && !val.isBlank();
    }
}
