/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.datalayer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Антон
 */
public class KinomirManager {

    private static final String DISCOUNT = "DISCOUNT";
    public static final String IDCLIENT = "IDCLIENT";
    private static final String IFDISCOUNT = "IFDISCOUNT";
    private static final String ALLFIELDS = "ALLFIELDS";
    private static final String ASID = "ASID";
    private static final String IDBUILDING = "IDBUILDING";
    private static final String IDGENRE = "IDGENRE";
    private static final String IDHALL = "IDHALL";
    private static final String IDPERFORMANCE = "IDPERFORMANCE";
    private static final String IDSHOW = "IDSHOW";
    private static final String IDSHOWTYPE = "IDSHOWTYPE";
    private static final String BEGINTIME = "BEGINTIME";
    private static final String ENDTIME = "ENDTIME";
    private static final String IDPLACE = "IDPLACE";
    public static final String IDORDER = "IDORDER";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String WEEKS = "WEEKS";
    private static final String WITHPRICE = "WITHPRICE";
    private static final String ALLPLACES = "ALLPLACES";
    private static final String IDPRICECATEGORY = "IDPRICECATEGORY";
    private static final String EMAIL = "EMAIL";
    private static final String IDUSER = "IDUSER";
    private static final String MARK = "MARK";
    private static final String IDPAYMENTMETHOD = "IDPAYMENTMETHOD";
    private static final String AMOUNT = "AMOUNT";

    public static ResultSet getOrderInfo(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_GetOrderInfo ?");
            if (params.get(IDORDER) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDORDER)));
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet addPayment(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_AddPayment ?, ?, ?, ?, ?");
            if (params.get(IDORDER) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDORDER)));
            }
            if (params.get(AMOUNT) != null) {
                sp.setFloat(2, Float.parseFloat(params.get(AMOUNT)));
            }
            if (params.get(IDPAYMENTMETHOD) != null) {
                sp.setInt(3, Integer.parseInt(params.get(IDPAYMENTMETHOD)));
            }
            if (params.get(MARK) != null) {
                sp.setString(4, params.get(MARK));
            }
            if (params.get(IDUSER) != null) {
                sp.setLong(5, Long.parseLong(params.get(IDUSER)));
            }
            if (params.get(IDCLIENT) != null) {
                sp.setInt(5, Integer.parseInt(params.get(IDCLIENT)));
            } else {
                sp.setNull(5, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet createOrder(Connection conn, Map<String, String> params, Logger logger) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_CreateOrder ?, ?, ?, ?");
            if (params.get(IDCLIENT) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDCLIENT)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }

            if (params.get(DESCRIPTION) != null) {
                try {
                    sp.setString(2, URLDecoder.decode(params.get(DESCRIPTION), "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    logger.error("Unable decode description field use empty string", ex);
                    sp.setString(2, "");
                }
            } else {
                sp.setNull(2, java.sql.Types.VARCHAR);
            }
            if (params.get(DISCOUNT) != null) {
                sp.setInt(3, Integer.parseInt(params.get(DISCOUNT)));
            } else {
                sp.setNull(3, java.sql.Types.INTEGER);
            }
            if ((params.get(IFDISCOUNT) != null) && (!(params.get(IFDISCOUNT)).isEmpty())) {
                int val = Integer.parseInt(params.get(IFDISCOUNT));
                sp.setBoolean(4, val > 0);
            } else {
                sp.setBoolean(4, false);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet dropPlace(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_DropPlace ?, ?");
            if (params.get(IDPERFORMANCE) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDPERFORMANCE)));
            }
            if (params.get(IDCLIENT) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDCLIENT)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getClientInfo(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_GetClientInfo ?, ?");
            if (params.get(EMAIL) != null) {
                sp.setString(1, params.get(EMAIL));
            } else {
                sp.setNull(1, java.sql.Types.VARCHAR);
            }
            if ((params.get(IDCLIENT) != null) && (!"".equals(params.get(IDCLIENT)))) {
                sp.setInt(2, Integer.parseInt(params.get(IDCLIENT)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getHallSchemaNC(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_GetHallSchemaNC ?, ?, ?");
            if ((params.get(IDHALL) != null) && (!"null".equalsIgnoreCase(params.get(IDHALL)))) {
                sp.setInt(1, Integer.parseInt(params.get(IDHALL)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            if ((params.get(IDPERFORMANCE) != null) && (!"null".equalsIgnoreCase(params.get(IDPERFORMANCE)))) {
                sp.setInt(2, Integer.parseInt(params.get(IDPERFORMANCE)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            if ((params.get(IDPRICECATEGORY) != null) && (!"null".equalsIgnoreCase(IDPRICECATEGORY))) {
                sp.setInt(3, Integer.parseInt(params.get(IDPRICECATEGORY)));
            } else {
                sp.setNull(3, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getPerfInfo(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_GetPerfInfo ?");

            if (params.get(IDPERFORMANCE) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDPERFORMANCE)));
            } else {
                throw new InvalidParameterException("Parameter '" + IDPERFORMANCE + "' not found!");
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getPerformanceNew(Connection conn, Map<String, String> params, Logger logger, DateFormat df) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_GetPerformancesNew ?, ?, ?, ?, ?, ?");
            if (params.get(IDBUILDING) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDBUILDING)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            if (params.get(IDSHOW) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDSHOW)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            if (params.get(IDGENRE) != null) {
                sp.setInt(3, Integer.parseInt(params.get(IDGENRE)));
            } else {
                sp.setNull(3, java.sql.Types.INTEGER);
            }
            setBeginTimeParameter(params, sp, df, logger, 4);
            setEndTimeParameter(params, sp, df, logger, 5);
            if (params.get(IDPERFORMANCE) != null) {
                sp.setInt(6, Integer.parseInt(params.get(IDPERFORMANCE)));
            } else {
                sp.setNull(6, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getPlaces(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            if (params.get(IDCLIENT) != null) {
                sp = conn.prepareStatement("exec dbo.Wga_GetPlaces ?, ?, ?, ?");
            } else {
                sp = conn.prepareStatement("exec Wga_GetPlacesNC ?, ?, ?");
            }
            if (params.get(IDPERFORMANCE) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDPERFORMANCE)));
            }
            if ((params.get(WITHPRICE) != null) && !(params.get(WITHPRICE).equalsIgnoreCase("null"))) {
                sp.setShort(2, Short.parseShort(params.get(WITHPRICE)));
            } else {
                sp.setNull(2, java.sql.Types.SMALLINT);
            }
            if ((params.get(ALLPLACES) != null) && !(params.get(ALLPLACES).equalsIgnoreCase("null"))) {
                sp.setShort(3, Short.parseShort(params.get(ALLPLACES)));
            } else {
                sp.setNull(3, java.sql.Types.SMALLINT);
            }
            if (params.get(IDCLIENT) != null) {
                sp.setInt(4, Integer.parseInt(params.get(IDCLIENT)));
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getPerformance(Connection conn, Map<String, String> params, Logger logger, DateFormat df) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_GetPerformance ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
            // @IdBuilding int, @IdHall int, @IdShow int, @IdShowType varchar(2), @IdGenre int, 
            // @BeginTime datetime, @EndTime datetime, @AllFields tinyint, @AsId bit
            if (params.get(IDBUILDING) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDBUILDING)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            if (params.get(IDHALL) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDHALL)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            if (params.get(IDSHOW) != null) {
                sp.setInt(3, Integer.parseInt(params.get(IDSHOW)));
            } else {
                sp.setNull(3, java.sql.Types.INTEGER);
            }
            if (params.get(IDSHOWTYPE) != null) {
                sp.setString(4, params.get(IDSHOWTYPE));
            } else {
                sp.setNull(4, java.sql.Types.VARCHAR);
            }
            if (params.get(IDGENRE) != null) {
                sp.setInt(5, Integer.parseInt(params.get(IDGENRE)));
            } else {
                sp.setNull(5, java.sql.Types.INTEGER);
            }
            setBeginTimeParameter(params, sp, df, logger, 6);
            setEndTimeParameter(params, sp, df, logger, 7);
            if (params.get(ALLFIELDS) != null) {
                sp.setInt(8, Integer.parseInt(params.get(ALLFIELDS)));
            } else {
                sp.setInt(8, 1);
            }
            if (params.get(ASID) != null) {
                sp.setInt(9, Integer.parseInt(params.get(ASID)));
            } else {
                sp.setInt(9, 1);
            }
            if (params.get(IDPERFORMANCE) != null) {
                sp.setInt(10, Integer.parseInt(params.get(IDPERFORMANCE)));
            } else {
                sp.setNull(10, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getShowNew(Connection conn, Map<String, String> params, Logger logger, DateFormat df) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_GetShow ?, ?, ?, ?, ?");

            if (params.get(IDBUILDING) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDBUILDING)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            if (params.get(IDGENRE) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDGENRE)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            setBeginTimeParameter(params, sp, df, logger, 3);
            setEndTimeParameter(params, sp, df, logger, 4);
            if (params.get(IDSHOW) != null) {
                sp.setInt(5, Integer.parseInt(params.get(IDSHOW)));
            } else {
                sp.setNull(5, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getShow(Connection conn, Map<String, String> params, Logger logger, DateFormat df) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_GetShow ?, ?, ?, ?, ?, ?, ?");

            if (params.get(IDBUILDING) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDBUILDING)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            if (params.get(IDHALL) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDHALL)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            if (params.get(IDSHOWTYPE) != null) {
                try {
                    sp.setString(3, new String(params.get(IDSHOWTYPE).getBytes("UTF-8"), "CP1251"));
                } catch (UnsupportedEncodingException ex) {
                    logger.error("Can't convert parametr to CP1251");
                }
            } else {
                sp.setNull(3, java.sql.Types.VARCHAR);
            }
            if (params.get(IDGENRE) != null) {
                sp.setInt(4, Integer.parseInt(params.get(IDGENRE)));
            } else {
                sp.setNull(4, java.sql.Types.INTEGER);
            }
            setBeginTimeParameter(params, sp, df, logger, 5);
            setEndTimeParameter(params, sp, df, logger, 6);
            if (params.get(IDSHOW) != null) {
                sp.setInt(7, Integer.parseInt(params.get(IDSHOW)));
            } else {
                sp.setNull(7, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getZUInfo(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_getzuinfo ?");
            if (params.get(IDBUILDING) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDBUILDING)));
            } else {
                throw new InvalidParameterException("Parameter '" + IDBUILDING + "' not found!");
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet lockPlace(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.WgA_LockPlace ?, ?, ?, ?");
            if (params.get(IDPERFORMANCE) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDPERFORMANCE)));
            }
            if (params.get(IDPLACE) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDPLACE)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            if (params.get(IDCLIENT) != null) {
                sp.setInt(3, Integer.parseInt(params.get(IDCLIENT)));
            } else {
                sp.setNull(3, java.sql.Types.INTEGER);
            }
            if ((params.get(IFDISCOUNT) != null) && (!(params.get(IFDISCOUNT)).isEmpty())) {
                int val = Integer.parseInt(params.get(IFDISCOUNT));
                sp.setBoolean(4, val > 0);
            } else {
                sp.setBoolean(4, false);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getOrders(Connection conn, Map<String, String> params, Logger logger, DateFormat df) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_GetOrders ?, ?");
            setBeginTimeParameter(params, sp, df, logger, 1);
            setEndTimeParameter(params, sp, df, logger, 2);
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getMoovieSoon(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_moviesoon ?");

            if (params.get(WEEKS) != null) {
                sp.setInt(1, Integer.parseInt(params.get(WEEKS)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet getShowInfo(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.MyWeb_ShowInfo ?");
            if (params.get(IDSHOW) != null) {
                sp.setInt(1, Integer.parseInt(params.get(IDSHOW)));
            } else {
                sp.setNull(1, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet setOrderDescription(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_SetOrderDescrioption ?, ?");
            if (params.get(IDORDER) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDORDER)));
            }
            if (params.get(DESCRIPTION) != null) {
                sp.setString(2, params.get(DESCRIPTION));
            } else {
                sp.setNull(2, java.sql.Types.VARCHAR);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet setOrderToNull(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_SetOrderToNull ?");
            if (params.get(IDORDER) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDORDER)));
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    public static ResultSet unlockPlace(Connection conn, Map<String, String> params) throws SQLException, InvalidParameterException {
        PreparedStatement sp = null;
        try {
            sp = conn.prepareStatement("exec dbo.Wga_UnlockPlace ?, ?, ?");
            if (params.get(IDPERFORMANCE) != null) {
                sp.setLong(1, Long.parseLong(params.get(IDPERFORMANCE)));
            }
            if (params.get(IDPLACE) != null) {
                sp.setInt(2, Integer.parseInt(params.get(IDPLACE)));
            } else {
                sp.setNull(2, java.sql.Types.INTEGER);
            }
            if (params.get(IDCLIENT) != null) {
                sp.setInt(3, Integer.parseInt(params.get(IDCLIENT)));
            } else {
                sp.setNull(3, java.sql.Types.INTEGER);
            }
            return sp.executeQuery();
        } finally {
            if (sp != null) {
                sp.close();
            }
        }
    }

    private static void setBeginTimeParameter(Map<String, String> params, PreparedStatement sp, DateFormat df, Logger logger, int place) throws SQLException {
        try {
            if (params.get(BEGINTIME) != null) {
                if (params.get(BEGINTIME).contains(":")) {
                    sp.setTimestamp(place, new Timestamp(df.parse(params.get(BEGINTIME)).getTime()));
                } else {
                    logger.debug("Converted begin date:" + df.format(df.parse(params.get(BEGINTIME) + " 00:00:00")));
                    sp.setTimestamp(place, new Timestamp(df.parse(params.get(BEGINTIME) + " 00:00:00").getTime()));
                }
            } else {
                sp.setNull(place, java.sql.Types.DATE);
            }
        } catch (Exception ex) {
            logger.error("Can't parse value for '" + BEGINTIME + "'", ex);
            sp.setNull(place, java.sql.Types.DATE);
        }
    }

    private static void setEndTimeParameter(Map<String, String> params, PreparedStatement sp, DateFormat df, Logger logger, int place) throws SQLException {
        try {
            if (params.get(ENDTIME) != null) {
                if (params.get(ENDTIME).contains(":")) {
                    sp.setTimestamp(place, new Timestamp(df.parse(params.get(ENDTIME)).getTime()));
                } else {
                    logger.debug("Converted end date:" + df.format(df.parse(params.get(ENDTIME) + " 23:59:59")));
                    sp.setTimestamp(place, new Timestamp(df.parse(params.get(ENDTIME) + " 23:59:59").getTime()));
                }
            } else {
                sp.setNull(place, java.sql.Types.DATE);
            }
        } catch (ParseException ex) {
            logger.error("Can't parse value for '" + ENDTIME + "'", ex);
            sp.setNull(place, java.sql.Types.DATE);
        }
    }
}
