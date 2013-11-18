/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.processing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
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
import javax.servlet.ServletConfig;
import javax.sql.DataSource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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

	protected static Connection getConnection() throws SQLException, NamingException {
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
			if (orderInfo.isOrderExists()) {
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
			}
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

	public static Purchase registerPayment(Long orderId, String bank_trx_id, String attributes, double amount, ServletConfig config, String rrn) {
		Connection conn = null;
		Map<String, String> paymentParams = new HashMap<String, String>();
		paymentParams.put(KinomirManager.IDORDER, orderId.toString());
		paymentParams.put(KinomirManager.AMOUNT, Double.toString(amount / 100d));
		paymentParams.put(KinomirManager.IDPAYMENTMETHOD, config.getInitParameter("paymentMethod"));
		paymentParams.put(KinomirManager.MARK, "RUR");
		paymentParams.put(KinomirManager.IDCLIENT, null);
		paymentParams.put(KinomirManager.BANKTRXID, bank_trx_id);
		paymentParams.put(KinomirManager.PAYATTRIBYTES, attributes);
		paymentParams.put(KinomirManager.RRN, rrn);
		try {
			conn = getConnection();
			AddPaymentResultDTO paymentResult = KinomirManager.addPayment(conn, paymentParams, logger);
			String resultDesc = paymentResult.getResultDescription();
			logger.debug("Register result: " + paymentResult.getResultDescription());
			Purchase res = new Purchase(amount, resultDesc, null, orderId);
			if ("0".equals(paymentResult.getResult())) {
				res.setResult(Purchase.REGISTERED);
			} else {
				//TODO: Надо сделать возврат денег
				try {
					logger.info("Try return payment to client");
					StringBuilder returnQueryString = new StringBuilder(config.getInitParameter("returnURL"));
					returnQueryString.append("?trx_id=").append(bank_trx_id);
					returnQueryString.append("&p.rrn=").append(rrn);
					returnQueryString.append("&amount=").append(Double.toString(amount));
					DefaultHttpClient client = new DefaultHttpClient();
					UsernamePasswordCredentials creds = new UsernamePasswordCredentials(config.getInitParameter("returnUser"), config.getInitParameter("returnPassword"));
					HttpGet httpget = new HttpGet(returnQueryString.toString());
					client.getCredentialsProvider().setCredentials(new AuthScope(httpget.getURI().getHost(), httpget.getURI().getPort()), creds);
					HttpResponse response = client.execute(httpget);
					HttpEntity entity = response.getEntity();
					BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
					StringBuilder answer = new StringBuilder();
					String line = in.readLine();
					while (line != null) {
						answer.append(line);
						line = in.readLine();
					}
					Document answerXML = DocumentHelper.parseText(answer.toString());
					String returnResult = answerXML.valueOf("//MerchantAPI/RefundRes/Result/code");
					if ("1".equals(returnResult)) {
						logger.info("Payment is returns to client");
					} else if ("2".equals(returnResult)) {
						logger.error("Unable return to client");
					}
				} catch (Exception ex) {
					logger.debug("Error while return payment", ex);
				}
				res.setResult(Purchase.PAYMENT_FAILED);
			}
			return res;
		} catch (NamingException ex) {
			logger.error("Error while register payment", ex);
		} catch (SQLException ex) {
			logger.error("Error while register payment", ex);
		} catch (Exception ex) {
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
				logger.info(String.format("Order %1$d in processing, drop it", new Object[]{idOrder}));
				try {
					OrderToNullDTO orderNullDTO = KinomirManager.setOrderToNull(conn, orderParams);
					if ("0".equals(orderNullDTO.getError())) {
						logger.info(String.format("Order %1$d is set to null", new Object[]{idOrder}));
						purch.setResult(Purchase.REGISTERED);
					} else {
						logger.error("Error while drop order " + idOrder.toString() + ": " + orderNullDTO.getErrorDescription());
					}
				} catch (Exception ex) {
					logger.error("Error while drop order id = " + idOrder.toString(), ex);
				}
			} else {
				logger.error("Order " + idOrder.toString() + " state is " + orderDTO.getOrderState().toString() + ", can't drop it");
				purch.setResult(Purchase.REGISTERED);

			}
		} catch (SQLException ex) {
			logger.error("Error while drop order " + idOrder.toString() + ". " + ex.getMessage());
			logger.debug("Error while drop order" + idOrder.toString() + ". ", ex);
		} catch (NamingException ex) {
			logger.error("Error while drop order" + idOrder.toString() + ". " + ex.getMessage());
			logger.debug("Error while drop order" + idOrder.toString() + ". ", ex);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException ex) {
				logger.error("Error while drop order", ex);
			}
		}
		return purch;
	}

	public static void sendSms(Long idOrder, String smsUrl, String userId, String password, String login, String smsName) {
		Connection conn = null;
		Map<String, String> params = new HashMap<String, String>();
		params.put(KinomirManager.IDORDER, idOrder.toString());
		try {
			logger.debug("before send SMS");
			conn = getConnection();
			OrderInfoDTO orderInfo = KinomirManager.getOrderInfo(conn, params);
			params.put(KinomirManager.IDCLIENT, orderInfo.getOrderInfo("idclient"));
			String description = orderInfo.getOrderInfo("description");
			if ((description != null) && (description.length() > 3)) {
				description = description.substring(description.length() - 4);
			}
			String begintime = orderInfo.getOrderInfo("performancestarttime");
			String building = orderInfo.getOrderInfo("building");
			ClientInfoDTO clientInfo = KinomirManager.getClientInfo(conn, params);
			String phone = clientInfo.getClientInfoField("Cellular");
			String smsText = String.format("%1$s, %2$s заказ №%3$d пароль: %4$s", new Object[]{building, begintime, idOrder, description});
			if (null != phone && !"".equals(phone)) {
				if (phone.matches("7\\d{9}")) {
					logger.info(String.format("Send SMS to %1$s with text: %2$s", new Object[]{phone, smsText}));
					Document xml = DocumentHelper.createDocument();
					xml.setXMLEncoding("windows-1251");
					Element requestElement = xml.addElement("request");
					requestElement.addElement("user_id").setText(userId);
					requestElement.addElement("user").setText(login);
					requestElement.addElement("pwd").setText(password);
					requestElement.addElement("command").setText("send");
					requestElement.addElement("phone").setText(phone);
					requestElement.addElement("message").setText(smsText);
					requestElement.addElement("id").setText(idOrder.toString());
					requestElement.addElement("valid").setText("5");
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					GregorianCalendar now = new GregorianCalendar();
					now.add(Calendar.HOUR_OF_DAY, -3);
					requestElement.addElement("schedule").setText(df.format(now.getTime()));
					requestElement.addElement("sender").setText(smsName);
					URLQuery.excutePost(smsUrl, xml.asXML());
				} else {
					logger.info("SMS text: " + smsText);
					logger.error("SMS was not sent. Error in number: " + phone);
				}
			} else {
				logger.error("SMS was not sent. NO PHONE NUMBER IN ORDER");
			}
			logger.debug("after send sms");
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

	public static void sendMail(Long idOrder, Double amount, int resultCode, String mailURL) {
		Map<String, String> requestParams = new HashMap<String, String>();
		requestParams.put("result_code", Integer.toString(resultCode));
		requestParams.put("amount", amount.toString());
		try {
			URLQuery.excutePost(mailURL + Long.toString(idOrder) + "/", requestParams);
		} catch (Exception ex) {
			logger.error("Error while send mail", ex);
		}
	}
}
