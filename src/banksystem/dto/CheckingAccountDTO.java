package banksystem.dto;

import java.util.Date;

/**
 * DTO pour CheckingAccount_Details joint avec BankAccount.
 */
public class CheckingAccountDTO extends AccountDTO {

    private int    monthlyFreeTX;
    private double txFeePercent;      // 30 signifie 30% du montant de la transaction
    private int    txUsedThisMonth;
    private String lastResetMonth;    // format YYYY-MM

    public CheckingAccountDTO() { super(); }

    public CheckingAccountDTO(long accountId, int customerId, double balance, String status,
                              double dailyLimit, Date openedDate,
                              int monthlyFreeTX, double txFeePercent,
                              int txUsedThisMonth, String lastResetMonth) {
        super(accountId, customerId, "CHECKING", balance, status, dailyLimit, openedDate);
        this.monthlyFreeTX    = monthlyFreeTX;
        this.txFeePercent     = txFeePercent;
        this.txUsedThisMonth  = txUsedThisMonth;
        this.lastResetMonth   = lastResetMonth;
    }

    public int    getMonthlyFreeTX()             { return monthlyFreeTX; }
    public void   setMonthlyFreeTX(int v)        { this.monthlyFreeTX = v; }

    public double getTxFeePercent()              { return txFeePercent; }
    public void   setTxFeePercent(double v)      { this.txFeePercent = v; }

    public int    getTxUsedThisMonth()           { return txUsedThisMonth; }
    public void   setTxUsedThisMonth(int v)      { this.txUsedThisMonth = v; }

    public String getLastResetMonth()            { return lastResetMonth; }
    public void   setLastResetMonth(String v)    { this.lastResetMonth = v; }

    /** Retourne true si la prochaine transaction entraînera des frais de dépassement. */
    public boolean isOverLimit() {
        return txUsedThisMonth >= monthlyFreeTX;
    }

    @Override
    public String toString() {
        return String.format("CheckingAccountDTO[%d] Solde=$%.2f TX:%d/%d Frais=%.0f%%",
            getAccountId(), getBalance(), txUsedThisMonth, monthlyFreeTX, txFeePercent);
    }
}
