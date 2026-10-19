package banksystem.dto;

import java.util.Date;

/**
 * DTO pour la table BankAccount.
 */
public class AccountDTO {

    private long   accountId;
    private int    customerId;
    private String accountType;   // "SAVINGS" | "CHECKING" | "CREDIT" | "CURRENCY"
    private double balance;
    private String status;        // "ACTIVE" | "FROZEN" | "CLOSED"
    private double dailyLimit;
    private Date   openedDate;

    public AccountDTO() {}

    public AccountDTO(long accountId, int customerId, String accountType,
                      double balance, String status, double dailyLimit, Date openedDate) {
        this.accountId   = accountId;
        this.customerId  = customerId;
        this.accountType = accountType;
        this.balance     = balance;
        this.status      = status;
        this.dailyLimit  = dailyLimit;
        this.openedDate  = openedDate;
    }

    public long   getAccountId()   { return accountId; }
    public void   setAccountId(long accountId)     { this.accountId = accountId; }

    public int    getCustomerId()  { return customerId; }
    public void   setCustomerId(int customerId)    { this.customerId = customerId; }

    public String getAccountType() { return accountType; }
    public void   setAccountType(String accountType) { this.accountType = accountType; }

    public double getBalance()     { return balance; }
    public void   setBalance(double balance)       { this.balance = balance; }

    public String getStatus()      { return status; }
    public void   setStatus(String status)         { this.status = status; }

    public double getDailyLimit()  { return dailyLimit; }
    public void   setDailyLimit(double dailyLimit) { this.dailyLimit = dailyLimit; }

    public Date   getOpenedDate()  { return openedDate; }
    public void   setOpenedDate(Date openedDate)   { this.openedDate = openedDate; }

    @Override
    public String toString() {
        return "AccountDTO [" + accountId + "] Customer=" + customerId
             + " Type=" + accountType + " Balance=$" + String.format("%.2f", balance)
             + " Status=" + status;
    }
}
