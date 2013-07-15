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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import ru.kinomir.datalayer.KinomirManager;
import ru.kinomir.datalayer.dto.AddPaymentResultDTO;
import ru.kinomir.datalayer.dto.ClientInfoDTO;
import ru.kinomir.datalayer.dto.OrderInfoDTO;
import ru.kinomir.datalayer.dto.OrderStatusDTO;
import ru.kinomir.datalayer.dto.OrderToNullDTO;
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
        Map<String, String> params = new HashMap<String, String>();
        params.put(KinomirManager.IDORDER, orderId.toString());
        params.put(KinomirManager.IDCLIENT, idClient != null ? idClient.toString() : null);
        try {
            conn = getConnection();
            OrderInfoDTO orderInfo = KinomirManager.getOrderInfo(conn, params);
            StringBuilder longDesc = new StringBuilder("Билеты на ").append(orderInfo.getOrderInfo(SHOWNAME_COLUMN)).append(" ");
            String curShow = orderInfo.getOrderInfo(SHOWNAME_COLUMN);
            Double amount = Double.parseDouble(orderInfo.getOrderInfo("ordertotalticketssum"));
            int count = 0;
            int countAll = 0;
            for (Map<String, String> oneOrderString : orderInfo.getOrderInfoValues()) {
                if (curShow.equals(oneOrderString.get(SHOWNAME_COLUMN))) {
                    count++;
                } else {
                    longDesc.append('(').append(count).append(')').append(". Билеты на ").append(oneOrderString.get(SHOWNAME_COLUMN)).append(" ");;
                    curShow = oneOrderString.get(SHOWNAME_COLUMN);
                    count = 1;
                }
                countAll++;
            }
            longDesc.append('(').append(count).append(')').append(". Итого ");
            if (countAll == 1) {
                longDesc.append(countAll).append(" билет");
            } else if (countAll > 1 && countAll < 5) {
                longDesc.append(countAll).append(" билета");
            } else {
                longDesc.append(countAll).append(" билетов");
            }
            String longDescStr = longDesc.toString();
            return new Purchase(amount, "Покупка билетов", longDescStr, orderId);

        } catch (SQLException ex) {
            logger.error("Error while register payment", ex);
        } catch (NamingException ex) {
            logger.error("Error while register payment", ex);
        } finally {
            try {
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
        Map<String, String> paymentParams = new HashMap<String, String>();
        paymentParams.put(KinomirManager.IDORDER, orderId.toString());
        paymentParams.put(KinomirManager.AMOUNT, Double.toString(amount / 100d));
        paymentParams.put(KinomirManager.IDPAYMENTMETHOD, Integer.toString(idTypePayment));
        paymentParams.put(KinomirManager.MARK, "RUR");
        paymentParams.put(KinomirManager.IDCLIENT, null);
        paymentParams.put(KinomirManager.BANKTRXID, bank_trx_id);
        paymentParams.put(KinomirManager.PAYATTRIBYTES, attributes);
        try {
            conn = getConnection();
            AddPaymentResultDTO paymentResult = KinomirManager.addPayment(conn, paymentParams);
            String resultDesc = paymentResult.getResultDescription();
            Purchase res = new Purchase(amount, resultDesc, null, orderId);
            if ("0".equals(paymentResult.getResult())) {
                res.setResult(Purchase.REGISTERED);
            } else {
                res.setResult(Purchase.PAYMENT_FAILED);
            }
            return res;
        } catch (NamingException ex) {
            logger.error("Error while register payment", ex);
        } catch (SQLException ex) {
            logger.error("Error while register payment", ex);
        } finally {
            try {
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
        Purchase purch = new Purchase(0d, "", "", idOrder);
        purch.setResult(Purchase.PAYMENT_FAILED);
        try {
            conn = getConnection();
            Map<String, String> orderParams = new HashMap<String, String>();
            orderParams.put(KinomirManager.IDORDER, idOrder.toString());
            OrderStatusDTO orderDTO = KinomirManager.getOrderStatus(conn, orderParams);
            if (orderDTO.getOrderState() <= 1) {
                logger.info("Order in processing, drop it");
                try {
                    OrderToNullDTO orderNullDTO = KinomirManager.setOrderToNull(conn, orderParams);
                    if ("0".equals(orderNullDTO.getError())) {
                        logger.info("Order is set to null");
                        purch.setResult(Purchase.REGISTERED);
                    } else {
                        logger.error("Error while drop order: " + orderNullDTO.getErrorDescription());
                    }
                } catch (Exception ex) {
                    logger.error("Error while drop order", ex);
                }
            } else {
                logger.error("Order state is " + orderDTO.getOrderState().toString() + ", can't drop it");
                purch.setResult(Purchase.REGISTERED);

            }
        } catch (SQLException ex) {
            logger.error("Error while drop order", ex);
        } catch (NamingException ex) {
            logger.error("Error while drop order", ex);
        } finally {
            try {
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
        Map<String, String> params = new HashMap<String, String>();
        params.put(KinomirManager.IDORDER, idOrder.toString());
        try {
            conn = getConnection();
            OrderInfoDTO orderInfo = KinomirManager.getOrderInfo(conn, params);
            params.put(KinomirManager.IDCLIENT, orderInfo.getOrderInfo("idclient"));
            String description = orderInfo.getOrderInfo("description");
            if ((description != null) && (description.length() > 3)) {
                description = description.substring(description.length() - 4);
            }
            String begintime = orderInfo.getOrderInfo("begintime");
            String building = orderInfo.getOrderInfo("building");
            ClientInfoDTO clientInfo = KinomirManager.getClientInfo(conn, params);
            String phone = clientInfo.getClientInfoField("Cellular");
            Document xml = DocumentHelper.createDocument();
            xml.setXMLEncoding("windows-1251");
            Element requestElement = xml.addElement("request");
            requestElement.addElement("user_id").setText(userId);
            requestElement.addElement("user").setText(login);
            requestElement.addElement("pwd").setText(password);
            requestElement.addElement("command").setText("send");
            requestElement.addElement("phone").setText(phone);
            requestElement.addElement("message").setText(String.format("%1$s, %2$s заказ №%3$d пароль: %4$s", new Object[]{building, begintime, idOrder, description}));
            requestElement.addElement("id").setText(idOrder.toString());
            requestElement.addElement("valid").setText("5");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            GregorianCalendar now = new GregorianCalendar();
            now.add(Calendar.HOUR_OF_DAY, -3);
            requestElement.addElement("schedule").setText(df.format(now.getTime()));
            requestElement.addElement("sender").setText("Kinomir");
            URLQuery.excutePost(smsUrl, xml.asXML());
        } catch (Exception ex) {
            logger.error("Error while send sms", ex);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.error("Error while send SMS", ex);
            }
        }
    }
}
