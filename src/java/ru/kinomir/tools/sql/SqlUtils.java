/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.tools.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Антон
 */
public class SqlUtils {

    public static SQLException convertErrorToException(ResultSet rs, SQLException originalEx) {
        SQLException result = null;
        try {
            result = new SQLException(rs.getString("ErrorDescription"), rs.getString("Error"), originalEx);
        } catch (SQLException ex) {
            result = originalEx;
        }
        return result;
    }

    public static void closeSQLObjects(ResultSet rs, PreparedStatement sp) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (sp != null) {
                sp.close();
            }
        } catch (SQLException ex) {
        }
    }
}
