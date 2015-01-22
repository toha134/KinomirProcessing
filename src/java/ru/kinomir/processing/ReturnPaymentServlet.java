/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.processing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.conn.BasicClientConnectionManager;

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
     * Возвращяет объект HttpClient с установленными свойствами для авторизации
     * по сертификатам
     *
     * @param keyStorePath путь к хранилищу сертификатов (файл p12 или jks)
     * @param keyPass пароль от хранилища ключей и сертификатов
     * @param ksType тип хранилища сертификатов PKCS12 или JKS
     * @param trustStorePath путь к хранилищу доверенных сертификатов (файл p12
     * или jks)
     * @param trustPass пароль от хранилища сертификатов
     * @param tsType тип хранилища сертификатов
     * @return
     * @throws KeyStoreException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public static HttpClient createSslHttpClient(String keyStorePath, String keyPass, String ksType, String trustStorePath, String trustPass, String tsType) throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException, NoSuchProviderException {
        KeyStore trustStore = null;
        KeyStore keyStore = null;
        KeyManagerFactory kmf = null;
        TrustManagerFactory tmf = null;
        X509TrustManager tm = null;
        X509HostnameVerifier verifier = new X509HostnameVerifier() {
            @Override
            public void verify(String string, SSLSocket ssls) throws IOException {
            }

            @Override
            public void verify(String string, X509Certificate xc) throws SSLException {
            }

            @Override
            public void verify(String string, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String string, SSLSession ssls) {
                return true;
            }
        };
        FileInputStream instream;
        if (trustStorePath != null) {
            trustStore = KeyStore.getInstance(tsType);
            instream = new FileInputStream(new File(trustStorePath));
            try {
                trustStore.load(instream, trustPass.toCharArray());
                tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
                tmf.init(trustStore);
            } finally {
                instream.close();
            }
        } else {
            tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
        }
        if (keyStorePath != null) {
            keyStore = KeyStore.getInstance(ksType);
            instream = new FileInputStream(new File(keyStorePath));
            try {
                keyStore.load(instream, keyPass.toCharArray());
                kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
                kmf.init(keyStore, keyPass.toCharArray());
            } finally {
                instream.close();
            }
        }
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(kmf != null ? kmf.getKeyManagers() : null, tmf != null ? tmf.getTrustManagers() : new TrustManager[]{tm}, null);
        SSLSocketFactory sf = new SSLSocketFactory(
                sslcontext,
                verifier);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", 443, sf.getSocketFactory()));
        ClientConnectionManager connMrg = new BasicClientConnectionManager(schemeRegistry);
        DefaultHttpClient httpclient = new DefaultHttpClient(connMrg);
        return httpclient;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
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
            final String orderId = request.getParameter("id_order");
            logger.info(String.format("[%1$s] Try return payment to client", orderId));
            logger.info(String.format("[%1$s] Request string: [%2$s]", orderId, request.getQueryString()));
            Map<String, String> params = new HashMap<String, String>();
            params.put(KinomirManager.IDORDER, orderId);
            connection = PurchaseMemento.getConnection();
            OrderInfoDTO orderInfo = KinomirManager.getOrderInfo(connection, params);
            StringBuilder returnQueryString = new StringBuilder(getInitParameter("returnURL"));
            if (orderInfo.isOrderExists()) {
                returnQueryString.append("?trx_id=").append(orderInfo.getOrderInfo("paydocnum"));
                returnQueryString.append("&p.rrn=").append(orderInfo.getOrderInfo("rrn"));
                returnQueryString.append("&amount=").append(Math.round(Double.parseDouble(orderInfo.getOrderInfo("amount")) * 100));
                DefaultHttpClient client = (DefaultHttpClient) createSslHttpClient(getInitParameter("ksPath"), getInitParameter("ksPass"), getInitParameter("ksType"), getInitParameter("tsPath"), getInitParameter("tsPass"), getInitParameter("tsType"));
                UsernamePasswordCredentials creds = new UsernamePasswordCredentials(getInitParameter("returnUser"), getInitParameter("returnPassword"));
                HttpGet httpget;
                logger.info(String.format("[%1$s] Return query: %2$s", orderId, returnQueryString.toString()));
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
                logger.info(String.format("[%1$s] Bank returns: %2$s", orderId, answer.toString()));
                Document answerXML = DocumentHelper.parseText(answer.toString());
                String returnResult = answerXML.valueOf("//MerchantAPI/Message/RefundResponse/Result/code");
                if (returnResult != null) {
                    if ("1".equals(returnResult)) {
                        logger.info(String.format("[%1$s] Payment successfully returned to client", orderId));
                        el.addAttribute("Error", "1");
                        el.addAttribute("ErrorDescription", "Success");
                    } else if ("2".equals(returnResult)) {
                        el.addAttribute("Error", "2");
                        el.addAttribute("ErrorDescription", "Unable return payment");
                    } else {
                        el.addAttribute("Error", returnResult);
                        el.addAttribute("ErrorDescription", answerXML.valueOf("//MerchantAPI/Message/RefundResponse/Result/desc"));
                    }
                } else {
                    el.addAttribute("Error", "-1");
                    el.addAttribute("ErrorDescription", "Error while return payment");
                }
            } else {
                logger.error(String.format("[%1$s] Order not found", orderId));
                el.addAttribute("Error", "2");
                el.addAttribute("ErrorDescription", "Unable return payment, no order for id");
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
     * Handles the HTTP <code>GET</code> method.
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
     * Handles the HTTP <code>POST</code> method.
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
