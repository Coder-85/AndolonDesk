package org.amjonota;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatData {
    private int protestNum;
    private int attended;
    private int upcoming;
    private int ongoing;
    private int missed;

    public StatData() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();

        String sqlTotal = "SELECT COUNT(*) as total FROM protests";
        int protestNum = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sqlTotal)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    protestNum = rs.getInt("total");
                }
            }
        }

        String currentDate = java.time.LocalDate.now().toString();

        String sqlAttending = "SELECT p.event_date FROM protests p " + "INNER JOIN attending_protests ap ON p.id = ap.protest_id " + "WHERE ap.user_id = ?";

        int attended = 0;
        int upcoming = 0;
        int ongoing = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sqlAttending)) {
            stmt.setInt(1, Session.getCurrentUser().getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String eventDate = rs.getString("event_date");

                    if (eventDate.compareTo(currentDate) < 0) {
                        attended++;
                    } else if (eventDate.compareTo(currentDate) > 0) {
                        upcoming++;
                    } else if (eventDate.equals(currentDate)) {
                        ongoing++;
                    }
                }
            }
        }

        int missed = protestNum - ongoing - upcoming - attended;

        this.protestNum = protestNum;
        this.attended = attended;
        this.upcoming = upcoming;
        this.ongoing = ongoing;
        this.missed = missed;
    }

    public int getProtestNum(){ return protestNum;}
    public int getAttended(){ return attended;}
    public int getUpcoming(){ return upcoming;}
    public int getOngoing(){ return ongoing;}
    public int getMissed(){ return missed;}
}
