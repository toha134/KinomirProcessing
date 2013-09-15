/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.datalayer.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.kinomir.tools.sql.SqlUtils;

/**
 *
 * @author Антон
 */
public class OrderInfoDTO {

    private static final String[] orderColumns = {"begintime", "brokerage", "timepayment",
        "orderexpiretime", "orderstate", "orderprice", "orderpaysum", "orderrecalltickets",
        "saledticketssum", "saledtickets", "ordertotalticketssum", "ordertotaltickets", "description", "idclient", "paydocnum", "rrn", "attributes"};
    private static final String[] performanceColumns = {"showname", "idperformance", "performancestarttime", "hall", "building"};
    private static final String[] placeColumns = {"idplace", "rownom", "placenom"};
    private List<Map<String, String>> orderInfoValues = new ArrayList<Map<String, String>>();
	boolean orderExists = false;

    public OrderInfoDTO() {
    }

    public OrderInfoDTO(ResultSet rs) throws SQLException {
        try {
            while (rs.next()) {
				orderExists = true;
                Map<String, String> orderInfoValue = new HashMap<String, String>();
                for (String orderCol : orderColumns) {
                    orderInfoValue.put(orderCol, rs.getString(orderCol));
                }
                for (String orderCol : performanceColumns) {
                    orderInfoValue.put(orderCol, rs.getString(orderCol));
                }
                for (String orderCol : placeColumns) {
                    orderInfoValue.put(orderCol, rs.getString(orderCol));
                }
                orderInfoValues.add(orderInfoValue);
            }
        } catch (SQLException ex) {
            throw SqlUtils.convertErrorToException(rs, ex);
        }
    }

	public boolean isOrderExists() {
		return orderExists;
	}
	
	

    public List<Map<String, String>> getOrderInfoValues() {
        return orderInfoValues;
    }

    public void setOrderInfoValues(List<Map<String, String>> orderInfoValues) {
        this.orderInfoValues = orderInfoValues;
    }
    
    public String getOrderInfo(String key){
        return orderInfoValues.get(0).get(key);
    }
}
