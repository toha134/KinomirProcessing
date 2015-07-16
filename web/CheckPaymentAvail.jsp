<%@page import="org.apache.log4j.Logger"%>
<%@page import="ru.kinomir.tools.RusToTranslit"%>
<%@page import="ru.kinomir.processing.PurchaseMemento"%>
<%@page import="ru.kinomir.processing.Purchase"%>
<%@page import="java.io.InputStream"%>
<%@page contentType="application/xml;charset=UTF-8" language="java" %>
<%
    final Long parameter = new Long(request.getParameter("o.mer_trx_id"));
    Long idClient = null;
    try {
      idClient = new Long(request.getParameter("o.idclient"));
    } catch (Exception ex) {
		
    }
    Logger logger = Logger.getLogger("ru.kinomir.processing");
    logger.info("Check payment query: " +request.getQueryString());
    Purchase purch = PurchaseMemento.getPurchase(parameter, idClient);
    String translitDesc = "Undefined";
    if (purch != null && purch.getLongDesc() != null) {
        translitDesc = RusToTranslit.convert(purch.getLongDesc());
    }
    logger.info("Check payment result ["+parameter+"] is: " + (purch != null ? "Payment is available" :"No order for payment"));
    String paymentAvail =
            "<payment-avail-response>"
            + "<result>"
            + "<code>" + (purch == null ? "2" : "1")
            + "</code>"
            + "<desc>" + (purch != null ? "Payment is available" : "There are no transactions with such id") + "</desc>"
            + "</result>"
            + "<merchant-trx>" + parameter + "</merchant-trx>"
            + "<purchase>"
            + "<shortDesc>" + (purch == null ? "Undefined" : purch.getDesc().substring(0, Math.min(purch.getDesc().length(), 30) - 1)) + "</shortDesc>"
            + "<longDesc>" + translitDesc + "</longDesc>"
            + "<account-amount>"
            + "<id>" + getInitParameter("accountId") + "</id>"
            + "<amount>" + (purch == null ? "0" : String.valueOf(purch.getAmountKop())) + "</amount>"
            + "<currency>643</currency>"
            + "<exponent>2</exponent>"
            + "</account-amount>"
            + "</purchase>"
            + "</payment-avail-response>";
    out.print(paymentAvail);
%>
