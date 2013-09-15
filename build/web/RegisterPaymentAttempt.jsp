<%@page import="org.apache.log4j.Logger"%>
<%@ page import="ru.kinomir.processing.Purchase,
         ru.kinomir.processing.PurchaseMemento"%>
<%@ page contentType="application/xml;charset=UTF-8" language="java" %>
<%
    Purchase purch = null;
    Logger logger = Logger.getLogger("ru.kinomir.processing");
    try {
        logger.info("Register query: " + request.getQueryString());
        if (request.getParameter("merchant_trx") != null){
            final Long trxId = new Long(request.getParameter("merchant_trx"));
            if ("1".equals(request.getParameter("result_code"))) {
                final double amount = Double.parseDouble(request.getParameter("amount"));
                final String bank_trx_id = request.getParameter("trx_id");
                logger.info("Try register success payment orderId = " + trxId);
                final String attributes = new StringBuilder().append(request.getParameter("merchant_trx")).append(" ").append(request.getParameter("p.cardholder")).append(" ").append(request.getParameter("ts")).toString();
                final String rrn = request.getParameter("p.rrn");
                purch = PurchaseMemento.registerPayment(trxId, bank_trx_id, attributes, amount, getServletConfig(), rrn);
                if ((null != purch) && (purch.getResult() == Purchase.REGISTERED)) {
                    logger.info("Register payment is  successfull, orderId = " + trxId);
                    if ("1".equals(getInitParameter("sendSMS"))) {
                        PurchaseMemento.sendSms(trxId, getInitParameter("smsUrl"), getInitParameter("smsUserId"), getInitParameter("smsPassword"), getInitParameter("smsLogin"));
                    }
                }
            } else {
                logger.info("Drop order orderId = " + trxId);
                purch = PurchaseMemento.dropOrder(trxId);
            }
        } else {
            logger.error("No 'merchant_trx' parametr in query");
        }
    } catch (Exception ex) {
        logger.error("Error while processing payment");
        logger.debug("Error extended info: " + ex.getMessage(), ex);
    } finally {
        int code;
        String desc;
        if ((null != purch) && (purch.getResult() == Purchase.REGISTERED)) {
            code = 1;
            desc = "Payment result successfully registered by the merchant";

        } else {
            code = 2;
            desc = "Unable to register payment result in merchant";
        }
        String registerPayment =
                "<register-payment-response>\n"
                + "    <result>\n"
                + "        <code>" + code + "</code>\n"
                + "        <desc>" + desc + "</desc>\n"
                + "    </result>\n"
                + "</register-payment-response>";

        out.print(registerPayment);
    }
%>
