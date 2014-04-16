/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.processing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

	private final static String QUEUE_NAME = "kinomir_sms";
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
			logger.error("Error while register payment:" + ex.getMessage());
			logger.debug("Error while register payment", ex);
		} catch (NamingException ex) {
			logger.error("Error while register payment" + ex.getMessage());
			logger.debug("Error while register payment", ex);
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
			Purchase res = new Purchase(amount, "", null, orderId);
			conn = getConnection();
			OrderInfoDTO orderInfo = null;
			try {
				orderInfo = KinomirManager.getOrderInfo(conn, paymentParams);
			} catch (SQLException ex) {
				logger.error("Unable to find order id = " + orderId + "Error is: " + ex.getMessage());
				logger.debug("Unable find order", ex);
			}
			if ((orderInfo != null) && (orderInfo.isOrderExists())) {

				AddPaymentResultDTO paymentResult = KinomirManager.addPayment(conn, paymentParams, logger);
				logger.debug("Register result: " + paymentResult.toString());
				String resultDesc = paymentResult.getResultDescription();
				res.setDesc(resultDesc);
				if ("0".equals(paymentResult.getResult())) {
					res.setResult(Purchase.REGISTERED);
					return res;
				} else {
					orderInfo = null;
				}

			}
			if ((orderInfo == null) || (!orderInfo.isOrderExists())) {
				//TODO: Надо сделать возврат денег
				try {
					logger.info("Try return payment to client");
					DefaultHttpClient client = (DefaultHttpClient) ReturnPaymentServlet.createSslHttpClient(config.getInitParameter("ksPath"), config.getInitParameter("ksPass"), config.getInitParameter("ksType"), config.getInitParameter("tsPath"), config.getInitParameter("tsPass"), config.getInitParameter("tsType"));
					StringBuilder returnQueryString = new StringBuilder(config.getInitParameter("returnURL"));
					returnQueryString.append("?trx_id=").append(bank_trx_id);
					returnQueryString.append("&p.rrn=").append(rrn);
					returnQueryString.append("&amount=").append(Double.toString(amount));
					logger.info("Refund request: " + returnQueryString.toString());
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
					logger.info("Bank answer: " + answer.toString());
					Document answerXML = DocumentHelper.parseText(answer.toString());
					String returnResult = answerXML.valueOf("//MerchantAPI/Message/RefundResponse/Result/code");
					if ("1".equals(returnResult)) {
						logger.info("Payment is returns to client");
						res.setResult(Purchase.PAYMENT_REVERSED);
					} else if ("2".equals(returnResult)) {
						logger.error("Unable return to client");
					} else {
						logger.error("Refund result code: " + returnResult);
						logger.error("Refund message: " + answerXML.valueOf("//MerchantAPI/Message/Result/desc"));
					}
				} catch (Exception ex) {
					logger.error("Error while return payment: " + ex.getMessage());
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
				purch.setResult(Purchase.CPA_REJECTED);

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
			DateFormat bdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			String begintime;
			try {
				begintime = bdf.format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(orderInfo.getOrderInfo("performancestarttime")));
			} catch (ParseException ex) {
				begintime = orderInfo.getOrderInfo("performancestarttime");
			}
			String building = orderInfo.getOrderInfo("building");
			ClientInfoDTO clientInfo = KinomirManager.getClientInfo(conn, params);
			String phone = clientInfo.getClientInfoField("Cellular");
			String smsText = String.format("%1$s, %2$s заказ №%3$d пароль: %4$s", new Object[]{building, begintime, idOrder, description});
			if (null != phone && !"".equals(phone)) {
				if (phone.matches("7\\d{10}")) {
					logger.info(String.format("Send SMS to %1$s with text: %2$s", new Object[]{phone, smsText}));
					ConnectionFactory factory = new ConnectionFactory();
					factory.setHost("localhost");
					com.rabbitmq.client.Connection connection = factory.newConnection();
					Channel channel = connection.createChannel();
					try {
						channel.queueDeclare(QUEUE_NAME, false, false, false, null);
						String message = "<message><to>" + phone + "</to><text>" + smsText + "</text><id>" + idOrder.toString() + "</id></message>";
						channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
						//System.out.println(" [x] Sent '" + message + "'");
					} finally {
						channel.close();
						connection.close();
					}
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
