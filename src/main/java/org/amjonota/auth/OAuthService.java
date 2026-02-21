package org.amjonota.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.amjonota.DatabaseManager;
import org.amjonota.model.User;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class OAuthService {
    public enum Provider { GOOGLE, FACEBOOK }

    // Google
    private static final String GOOGLE_CLIENT_ID;
    private static final String GOOGLE_CLIENT_SECRET;
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_SCOPE = "openid email profile";

    // Facebook
    private static final String FACEBOOK_CLIENT_ID;
    private static final String FACEBOOK_CLIENT_SECRET;
    private static final String FACEBOOK_AUTH_URL = "https://www.facebook.com/v19.0/dialog/oauth";
    private static final String FACEBOOK_TOKEN_URL = "https://graph.facebook.com/v19.0/oauth/access_token";
    private static final String FACEBOOK_USERINFO_URL = "https://graph.facebook.com/me?fields=id,name,email,birthday";
    private static final String FACEBOOK_SCOPE = "email,public_profile,user_birthday";

    private static final String REDIRECT_URI = "http://localhost:8080/callback";

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        Map<String, String> env = loadEnv();
        GOOGLE_CLIENT_ID = env.getOrDefault("GOOGLE_CLIENT_ID", "");
        GOOGLE_CLIENT_SECRET = env.getOrDefault("GOOGLE_CLIENT_SECRET", "");
        FACEBOOK_CLIENT_ID = env.getOrDefault("FACEBOOK_CLIENT_ID", "");
        FACEBOOK_CLIENT_SECRET = env.getOrDefault("FACEBOOK_CLIENT_SECRET", "");
    }

    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<String, String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        }
        catch (IOException e) {
            System.err.println("Could not load .env file! Reason: " + e.getMessage());
        }
        return map;
    }

    private static class CallbackHandler implements HttpHandler {
        private final AtomicReference<String> codeRef;
        private final AtomicReference<String> stateRef;
        private final CountDownLatch latch;

        CallbackHandler(AtomicReference<String> codeRef, AtomicReference<String> stateRef, CountDownLatch latch) {
            this.codeRef = codeRef;
            this.stateRef = stateRef;
            this.latch = latch;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            codeRef.set(extractParam(query, "code"));
            stateRef.set(extractParam(query, "state"));

            String html = "<html><body><h2>Authentication successful! You may close this tab.</h2></body></html>";
            exchange.sendResponseHeaders(200, html.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
            latch.countDown();
        }

        private String extractParam(String query, String key) {
            if (query == null) return null;
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && kv[0].equals(key)) return kv[1];
            }
            return null;
        }
    }

    public User startOAuthFlow(Provider provider) throws Exception {
        String state = UUID.randomUUID().toString();
        String authUrl = buildAuthUrl(provider, state);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> codeRef  = new AtomicReference<String>();
        AtomicReference<String> stateRef = new AtomicReference<String>();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/callback", new CallbackHandler(codeRef, stateRef, latch));
        server.start();

        Desktop.getDesktop().browse(new URI(authUrl));
        latch.await();
        server.stop(0);

        if (!state.equals(stateRef.get())) throw new Exception("OAuth state mismatch!");
        String code = codeRef.get();
        if (code == null) throw new Exception("No authorization code received");

        String accessToken = exchangeCodeForToken(provider, code);
        return fetchAndUpsertUser(provider, accessToken);
    }

    private String buildAuthUrl(Provider provider, String state) {
        String clientId, authUrl, scope;
        if (provider == Provider.GOOGLE) {
            clientId = GOOGLE_CLIENT_ID;
            authUrl = GOOGLE_AUTH_URL;
            scope = GOOGLE_SCOPE;
        }
        else {
            clientId = FACEBOOK_CLIENT_ID;
            authUrl = FACEBOOK_AUTH_URL;
            scope = FACEBOOK_SCOPE;
        }
        return authUrl
            + "?client_id=" + encode(clientId)
            + "&redirect_uri=" + encode(REDIRECT_URI)
            + "&response_type=code"
            + "&scope=" + encode(scope)
            + "&state=" + encode(state);
    }

    private String exchangeCodeForToken(Provider provider, String code) throws IOException, InterruptedException {
        String clientId, clientSecret, tokenUrl;
        if (provider == Provider.GOOGLE) {
            clientId = GOOGLE_CLIENT_ID;
            clientSecret = GOOGLE_CLIENT_SECRET;
            tokenUrl = GOOGLE_TOKEN_URL;
        } else {
            clientId = FACEBOOK_CLIENT_ID;
            clientSecret = FACEBOOK_CLIENT_SECRET;
            tokenUrl = FACEBOOK_TOKEN_URL;
        }

        String body = "client_id=" + encode(clientId)
            + "&client_secret=" + encode(clientSecret)
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(REDIRECT_URI)
            + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return extractJsonField(response.body(), "access_token");
    }

    private User fetchAndUpsertUser(Provider provider, String accessToken) throws IOException, InterruptedException, SQLException {
        String userinfoUrl = (provider == Provider.GOOGLE) ? GOOGLE_USERINFO_URL : FACEBOOK_USERINFO_URL;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(userinfoUrl))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();

        String providerId = extractJsonField(json, (provider == Provider.GOOGLE) ? "sub" : "id");
        String name = extractJsonField(json, "name");
        String email = extractJsonField(json, "email");
        String dob = extractJsonField(json, (provider == Provider.GOOGLE) ? "birthdate" : "birthday");
        String providerName = provider.name().toLowerCase();

        return upsertUser(providerId, name, email, dob, providerName);
    }

    private User upsertUser(String providerId, String name, String email, String dob, String providerName) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();

        String selectSql = "SELECT * FROM users WHERE provider = ? AND provider_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, providerName);
            stmt.setString(2, providerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String createdAt = rs.getString("created_at");
                    User user = new User(id, name, email, null, providerName, providerId, dob);
                    user.setCreatedAt(createdAt);
                    return user;
                }
            }
        }

        String createdAt = LocalDateTime.now().format(DATETIME_FORMAT);
        String insertSql = "INSERT INTO users (name, email, provider, provider_id, date_of_birth, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, providerName);
            stmt.setString(4, providerId);
            stmt.setString(5, dob);
            stmt.setString(6, createdAt);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                User user = new User(0, name, email, null, providerName, providerId, dob);
                if (keys.next()) {
                    user = new User(keys.getInt(1), name, email, null, providerName, providerId, dob);
                }
                user.setCreatedAt(createdAt);
                return user;
            }
        }
    }

    private String extractJsonField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return null;
        int start = json.indexOf('"', colon + 1);
        if (start == -1) return null;

        StringBuilder raw = new StringBuilder();
        int i = start + 1;

        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                raw.append(c).append(json.charAt(i + 1));
                i += 2;
            }
            else if (c == '"') {
                break;
            }
            else {
                raw.append(c);
                i++;
            }
        }

        return unescapeJsonString(raw.toString());
    }

    private String unescapeJsonString(String s) {
        if (s == null) return null;

        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; break;
                    case '\\': sb.append('\\'); i += 2; break;
                    case '/':  sb.append('/');  i += 2; break;
                    case 'n':  sb.append('\n'); i += 2; break;
                    case 'r':  sb.append('\r'); i += 2; break;
                    case 't':  sb.append('\t'); i += 2; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            String hex = s.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            }
                            catch (NumberFormatException e) {
                                sb.append(c);
                            }
                            i += 6;
                        }
                        else {
                            sb.append(c);
                            i++;
                        }
                        break;
                    default: sb.append(c); i++;
                }
            }
            else {
                sb.append(c);
                i++;
            }
        }

        return sb.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
