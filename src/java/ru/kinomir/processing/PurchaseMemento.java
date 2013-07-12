/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.processing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import ru.kinomir.datalayer.KinomirManager;
import ru.kinomir.tools.http.URLQuery;

/**
 *
 * @author Admin
 */
public class PurchaseMemento {

    private static final String SHOWNAME_COLUMN = "showname";
    private static final transient Logger logger = Logger.getLogger("ru.kinomir.processing.PurchaseMemento");

    private static Connection getConnection() throws SQLException, NamingException {
        Context ctx = new InitialContext();
        DataSource dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/processingDS");
        Connection conn = dataSource.getConnection();
        return conn;
    }

    public static Purchase getPurchase(Long orderId, Long idClient) {
        Connection conn = null;
        PreparedStatement sp = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            if (idClient != null) {
                sp = conn.prepareStatement("exec dbo.MyWeb_GetOrderInfo ?, ?");
            } else {
                sp = conn.prepareStatement("exec dbo.MyWeb_GetOrderInfo ?");
            }
            sp.setLong(1, orderId);
            if (idClient != null) {
                sp.setLong(2, idClient);
            }
            rs = sp.executeQuery();

            if (rs.next()) {
                StringBuilder longDesc = new StringBuilder("Билеты на ").append(rs.getString(SHOWNAME_COLUMN)).append(" ");
                String curShow = rs.getString(SHOWNAME_COLUMN);
                Double amount = rs.getDouble("ordertotalticketssum");
                int count = 0;
                do {
                    if (curShow.equals(rs.getString(SHOWNAME_COLUMN))) {
                        count++;
                    } else {
                        longDesc.append('(').append(count).append(')').append(". Билеты на ").append(rs.getString(SHOWNAME_COLUMN)).append(" ");;
                        count = 0;
                    }
                } while (rs.next());
                if (count == 1) {
                    longDesc.append(count).append(" билет");
                } else if (count > 1 && count < 5) {
                    longDesc.append(count).append(" билета");
                } else {
                    longDesc.append(count).append(" билетов");
                }
                String longDescStr = longDesc.toString();
                return new Purchase(amount, "Покупка билетов", longDescStr, orderId);
            } else {
                return null;
            }
        } catch (SQLException ex) {
            logger.error("Error while register payment", ex);
        } catch (NamingException ex) {
            logger.error("Error while register payment", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (sp != null) {
                    sp.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.error("Error while register payment", ex);
            }
        }
        return null;
    }

    public static Purchase registerPayment(Long orderId, String bank_trx_id, String attributes, double amount, int idTypePayment) {
        Connection conn = null;
        PreparedStatement sp = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            sp = conn.prepareStatement("exec dbo.Wga_AddPayment ?, ?, ?, ?, ?, ?, ?");
            sp.setLong(1, orderId);
            sp.setDouble(2, amount / 100d);
            sp.setInt(3, idTypePayment);
            sp.setString(4, "RUR");
            sp.setNull(5, java.sql.Types.VARCHAR);
            sp.setString(6, bank_trx_id);
            sp.setString(7, attributes);
            rs = sp.executeQuery();
            if (rs.next()) {
                String resultDesc = null;
                try {
                    resultDesc = rs.getString("ResultDescription");
                } catch (Exception ex) {
                    logger.error("Error while register payment", ex);
                    resultDesc = "";
                }
                Purchase res = new Purchase(amount, resultDesc, null, orderId);
                if (rs.getInt("Result") == 0) {
                    res.setResult(Purchase.REGISTERED);
                    return res;
                } else {
                    res.setResult(Purchase.PAYMENT_FAILED);
                }
                return res;
            }
        } catch (SQLException ex) {
            logger.error("Error while register payment", ex);
        } catch (NamingException ex) {
            logger.error("Error while register payment", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (sp != null) {
                    sp.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.error("Error while register payment", ex);
            }
        }
        return null;
    }

    public static Purchase dropOrder(Long idOrder) {
        Connection conn = null;
        PreparedStatement sp = null;
        ResultSet rs = null;
        Purchase purch = new Purchase(0d, "", "", idOrder);
        purch.setResult(Purchase.PAYMENT_FAILED);
        try {
            conn = getConnection();
            sp = conn.prepareStatement("exec dbo.Wga_GetOrderStatus ?");
            sp.setLong(1, idOrder);
            rs = sp.executeQuery();
            if (rs.next()) {
                if (rs.getInt("OrderState") <= 1) {
                    rs.close();
                    sp.close();
                    logger.info("Order in processing, can drop it");
                    sp = conn.prepareStatement("exec dbo.Wga_SetOrderToNull ?");
                    sp.setLong(1, idOrder);
                    rs = sp.executeQuery();
                    if (rs.next()) {
                        try {
                            logger.info("Order is set to null");
                            purch.setResult(Purchase.REGISTERED);
                            return purch;
                        } catch (Exception ex) {
                            logger.error("Error while drop order", ex);
                        }
                    }
                } else {
                    logger.error("Order state is " + rs.getInt("OrderState") + " can't drop it");
                    purch.setResult(Purchase.REGISTERED);
                    return purch;
                }
            }
        } catch (SQLException ex) {
            logger.error("Error while drop order", ex);
        } catch (NamingException ex) {
            logger.error("Error while drop order", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (sp != null) {
                    sp.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.error("Error while drop order", ex);
            }
        }
        //logger.error("Payment error for order " + idOrder.toString());
        return purch;
    }

    public static void sendSms(Long idOrder, String smsUrl, String userId, String password, String login) {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            Map<String, String> params = new HashMap<String, String>();
            params.put(KinomirManager.IDORDER, idOrder.toString());
            rs = KinomirManager.getOrderInfo(conn, params);
            if (rs.next()){
                params.put(KinomirManager.IDCLIENT, rs.getString("idclient"));
                String description = rs.getString("description");
                if (description.length()>3){
                    description = description.substring(description.length()-4);
                }
                String begintime = rs.getString("begintime");
                String building = rs.getString("building");
                rs.close();
                rs = KinomirManager.getClientInfo(conn, params);
                if (rs.next()){
                    String phone = rs.getString("Cellular");
                    rs.close();
                    Document xml = DocumentHelper.createDocument();
                    xml.setXMLEncoding("win-1251");
                    Element requestElement = xml.addElement("request");
                    requestElement.addElement("user_id").setText(userId);
                    requestElement.addElement("user").setText(login);
                    requestElement.addElement("pwd").setText(password);
                    requestElement.addElement("command").setText("send");
                    requestElement.addElement("phone").setText(phone);
                    requestElement.addElement("message").setText(String.format("%1, %2 заказ №%3 пароль: %4",  new Object[]{building, begintime, idOrder, description}));
                    requestElement.addElement("id").setText(idOrder.toString());
                    requestElement.addElement("valid").setText("5");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    GregorianCalendar now = new GregorianCalendar();
                    now.add(Calendar.HOUR_OF_DAY, -3);
                    requestElement.addElement("schedule").setText(df.format(now.getTime()));
                    requestElement.addElement("sender").setText("Kinomir");
                    URLQuery.excutePost(smsUrl, xml.asXML());
                }
                
            }
        } catch (Exception ex) {
            logger.error("Error while drop order", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.error("Error while send SMS", ex);
            }
        }
    }
}
