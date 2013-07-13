/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.datalayer.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import ru.kinomir.tools.sql.SqlUtils;

/**
 *
 * @author Антон
 */
public class OrderToNullDTO {

    private String error;
    private String errorDescription;

    public OrderToNullDTO() {
    }

    public OrderToNullDTO(ResultSet rs) throws SQLException {
        try {
            while (rs.next()) {
                error = rs.getString("Error");
                errorDescription = rs.getString("ErrorDescription");
            }
        } catch (SQLException ex) {
            throw SqlUtils.convertErrorToException(rs, ex);
        }
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
