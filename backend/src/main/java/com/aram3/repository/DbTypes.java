package com.aram3.repository;

import java.sql.ResultSet;
import java.sql.SQLException;

/** SQLite JDBC may return Integer/Long/Short for INTEGER columns — normalize safely. */
public final class DbTypes {
    private DbTypes() {}

    public static Long getLong(ResultSet rs, String column) throws SQLException {
        Object v = rs.getObject(column);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        String s = v.toString();
        if (s.isBlank()) {
            return null;
        }
        return Long.parseLong(s);
    }

    public static Integer getIntObj(ResultSet rs, String column) throws SQLException {
        Object v = rs.getObject(column);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = v.toString();
        if (s.isBlank()) {
            return null;
        }
        return Integer.parseInt(s);
    }

    public static int getInt(ResultSet rs, String column) throws SQLException {
        Integer v = getIntObj(rs, column);
        return v == null ? 0 : v;
    }
}
