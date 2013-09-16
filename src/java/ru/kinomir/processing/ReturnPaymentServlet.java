/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import ru.kinomir.datalayer.KinomirManager;
import ru.kinomir.datalayer.dto.OrderInfoDTO;

/**
 *
 * @author Антон
 */
public class ReturnPaymentServlet extends HttpServlet {

    private static final transient Logger logger = Logger.getLogger("ru.kinomir.processing");

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Connection connection = null;
        Document resXML = DocumentHelper.createDocument();
        Element el = resXML.addElement("data");
        try {
            logger.info("Try return payment to client");
			logger.info("Request string: " + request.getQueryString());

            Map<String, String> params = new HashMap<String, String>();
			params.put(KinomirManager.IDORDER, request.getParameter("id_order"));
            connection = PurchaseMemento.getConnection();
            OrderInfoDTO orderInfo = KinomirManager.getOrderInfo(connection, params);
            StringBuilder returnQueryString = new StringBuilder(getInitParameter("returnURL"));
            if (orderInfo.isOrderExists()) {
                returnQueryString.append("?trx_id=").append(orderInfo.getOrderInfo("paydocnum"));
                returnQueryString.append("&p.rrn=").append(orderInfo.getOrderInfo("rrn"));
                returnQueryString.append("&amount=").append(orderInfo.getOrderInfo("amount"));
                DefaultHttpClient client = new DefaultHttpClient();
                UsernamePasswordCredentials creds = new UsernamePasswordCredentials(getInitParameter("returnUser"), getInitParameter("returnPassword"));
                HttpGet httpget;
                httpget = new HttpGet(returnQueryString.toString());
                client.getCredentialsProvider().setCredentials(new AuthScope(httpget.getURI().getHost(), httpget.getURI().getPort()), creds);
                HttpResponse bankResponse;
                bankResponse = client.execute(httpget);
                HttpEntity entity = bankResponse.getEntity();
                BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                StringBuilder answer = new StringBuilder();
                String line = in.readLine();
                while (line != null) {
                    answer.append(line);
                    line = in.readLine();
                }
				logger.info("Bank returns: " + answer.toString());
                Document answerXML = DocumentHelper.parseText(answer.toString());
                String returnResult = answerXML.valueOf("//MerchantAPI/RefundRes/Result/code");
                if ("1".equals(returnResult)) {
                    logger.info("Payment is returns to client");
                    el.addAttribute("Error", "1");
                    el.addAttribute("ErrorDescription", "Success");
                } else if ("2".equals(returnResult)) {
                    el.addAttribute("Error", "2");
                    el.addAttribute("ErrorDescription", "Unable return payment");
                }
            } else {
				logger.error("Order not found");
				el.addAttribute("Error", "2");
				el.addAttribute("ErrorDescription", "Unable return payment, no order id");
			}

        } catch (NamingException ex) {
            logger.error("Unable to get DB connection");
			logger.debug(ex.getMessage(), ex);
            el.addAttribute("Error", "2");
            el.addAttribute("ErrorDescription", "Unable return payment");
        } catch (SQLException ex) {
            logger.error("Unable to get order info");
			logger.debug(ex.getMessage(), ex);
            el.addAttribute("Error", "2");
            el.addAttribute("ErrorDescription", "Unable return payment");
        } catch (InvalidParameterException ex) {
            logger.error("Unable parse bank answer");
			logger.debug(ex.getMessage(), ex);
            el.addAttribute("Error", "2");
            el.addAttribute("ErrorDescription", "Unable return payment");
        } catch (DocumentException ex) {
            logger.error("Unable parse bank answer");
			logger.debug(ex.getMessage(), ex);
            el.addAttribute("Error", "2");
            el.addAttribute("ErrorDescription", "Unable return payment");
        } catch (Exception ex) {
            logger.error("Unknown error");
			logger.debug(ex.getMessage(), ex);
            el.addAttribute("Error", "2");
            el.addAttribute("ErrorDescription", "Unable return payment");
        } finally {
            out.print(resXML.asXML());
            out.close();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
