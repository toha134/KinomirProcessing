/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.datalayer.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import ru.kinomir.tools.sql.SqlUtils;

/**
 *
 * @author Антон
 */
public class ClientInfoDTO {

    private final static String[] columns = {"idclient", "iddocument", "F", "I", "O", "address", "phone",
        "secaddress", "city", "email", "fax", "addstring", "Cellular", "BeginTime", "BarCode", "AccSkidka",
        "Description", "Description2", "Description3", "Birthday", "Login", "DocName", "OperationLimit", "OrderLife",
        "OrderLifeBeforePerformance", "IdSchBA", "ShowLimit", "PerformanceLimit", "UseAccSkidka", "DiskountName", "Percent", "isBlocFastSale"};
    Map<String, String> clientInfoValues = new HashMap<String, String>();

    public ClientInfoDTO() {
    }

    public ClientInfoDTO(ResultSet rs) throws SQLException {
        try {
            while (rs.next()) {
                for (String column : columns) {
                    try {
                        clientInfoValues.put(column, rs.getObject(column) == null ? "" : rs.getString(column));
                    } catch (SQLException ex) {
                    }
                }
            }
        } catch (SQLException ex) {
            throw SqlUtils.convertErrorToException(rs, ex);
        }
    }

    public String getClientInfoField(String key) {
        return clientInfoValues.get(key);
    }
}
