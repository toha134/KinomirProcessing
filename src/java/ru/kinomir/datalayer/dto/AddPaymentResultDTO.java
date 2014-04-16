/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.datalayer.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import ru.kinomir.tools.sql.SqlUtils;

/**
 *
 * @author Антон
 */
public class AddPaymentResultDTO {

	private String resultDescription = "";
	private String result = "";
	private String paymentId = "";

	public AddPaymentResultDTO() {
	}

	public AddPaymentResultDTO(ResultSet rs, Logger logger) throws SQLException {
		try {
			if (rs.next()) {
				result = rs.getString("Result");
				resultDescription = rs.getString("ResultDescription");
				paymentId = rs.getString("IdPayment");
				do {
					if (rs.getMetaData().getColumnCount() == 3) {
						logger.debug(String.format("%1$s %2$s %3$s", new Object[]{rs.getString(1), rs.getString(2), rs.getString(3)}));
					} else {
						for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
							logger.debug(rs.getMetaData().getColumnName(i) + " = " + rs.getString(i));
						}
					}
				} while (rs.next());
			} else {
				result = "1";
				resultDescription = "No result from SP";
			}
		} catch (SQLException ex) {
			try {
				resultDescription = rs.getString("ErrorDescription");
				result = "2";
			} catch (Exception er) {
				throw SqlUtils.convertErrorToException(rs, ex);
			}
		}
	}

	public String getResultDescription() {
		return resultDescription;
	}

	public void setResultDescription(String resultDescription) {
		this.resultDescription = resultDescription;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("PaymentResult[result = ").append(result)
				.append(", description = ").append(resultDescription).append(", idPayment = ")
				.append(paymentId).append("]").toString();
	}
}
