<%@page import="org.apache.log4j.Logger"%>
<%@ page import="ru.kinomir.processing.Purchase,
         ru.kinomir.processing.PurchaseMemento"%>
<%@ page contentType="application/xml;charset=UTF-8" language="java" %>
<%
    Purchase purch = null;
    Logger logger = Logger.getLogger("ru.kinomir.processing");
    try {
        logger.info("Register query: " + request.getQueryString());
        final Long trxId = new Long(request.getParameter("merchant_trx"));
        final double amount = Double.parseDouble(request.getParameter("amount"));
        int paymentMethod = Integer.parseInt(getInitParameter("paymentMethod"));
        if ("1".equals(request.getParameter("result_code"))) {
            final String bank_trx_id = request.getParameter("trx_id");
            logger.info("Register success payment orderId = " + trxId);
            final String attributes = new StringBuilder().append(request.getParameter("merchant_trx")).append(" ").append(request.getParameter("p.cardholder")).append(" ").append(request.getParameter("ts")).toString();
            purch = PurchaseMemento.registerPayment(trxId, bank_trx_id, attributes, amount, paymentMethod);
            if ((null != purch) && (purch.getResult() == Purchase.REGISTERED)) {
                if ("1".equals(getInitParameter("sendSMS"))) {
                    PurchaseMemento.sendSms(trxId, getInitParameter("smsUrl"), getInitParameter("smsUserId"), getInitParameter("smsPassword"), getInitParameter("smsLogin"));
                }
            }
        } else {
            logger.info("Drop order orderId = " + trxId);
            purch = PurchaseMemento.dropOrder(trxId);
        }
    } catch (Exception ex) {
        logger.error("Error while processing payment", ex);
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
