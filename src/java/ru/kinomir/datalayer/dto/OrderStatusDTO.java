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
public class OrderStatusDTO {

    private Integer orderState = null;

    public OrderStatusDTO() {
    }

    public OrderStatusDTO(ResultSet rs) throws SQLException {
        try {
            while (rs.next()) {
                orderState = rs.getInt("OrderState");
            }
        } catch (SQLException ex) {
            throw SqlUtils.convertErrorToException(rs, ex);
        }
    }

    public Integer getOrderState() {
        return orderState;
    }

    public void setOrderState(Integer orderState) {
        this.orderState = orderState;
    }
}
