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
public class AddPaymentResultDTO {

    private String resultDescription = "";
    private String result = "";
    private String paymentId = "";

    public AddPaymentResultDTO() {
    }

    public AddPaymentResultDTO(ResultSet rs) throws SQLException {
        try {
            while (rs.next()) {
                result = rs.getString("Result");
                resultDescription = rs.getString("ResultDescription");
                paymentId = rs.getString("IdPayment");
            }
        } catch (SQLException ex) {
            throw SqlUtils.convertErrorToException(rs, ex);
        }
    }

    public String getResultDescription() {
        return resultDescription;
    }

    public void setResultDescription(String resultDescription) {
        this.resultDescription = resultDescription;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}
