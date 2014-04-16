/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.processing;

/**
 *
 * @author Admin
 */
public class Purchase {

    public static final int REGISTERED = 0;
    public static final int CPA_REJECTED = 2;
    public static final int CPA_NONE = 21;
    public static final int CPA_FAILED = 4;
    public static final int CLIENT_LOST = 53;
    public static final int USER_CANCEL = 54;
    public static final int PAYMENT_REJECTED = -2;
    public static final int PAYMENT_FAILED = -3;
    public static final int PAYMENT_REVERSED = -4;
    private String desc;
    private Double amount;
    private String longDesc;
    private int result;
    private Long id;

    public Long getId() {
        return id;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int resilt) {
        this.result = resilt;
    }

    public Purchase(Double amount, String desc, String longDesc, Long id) {
        this.amount = amount;
        this.desc = desc;
        this.longDesc = longDesc;
		this.id = id;
    }

    public Double getAmount() {
        return amount;
    }
	
	public Integer getAmountKop() {
        return Math.round(amount.floatValue()*100);
    }

    public String getDesc() {
        return desc;
    }

    public String getLongDesc() {
        return longDesc;
    }

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
}
