package banksystem.dto;

import java.util.Date;

/**
 * DTO pour la table Transaction — piste d'audit complète incluant gains et pénalités.
 */
public class TransactionDTO {

    private long   transactionId;
    private long   accountId;
    private int    customerId;
    private String transactionType;   // DEPOSIT | WITHDRAWAL | TRANSFER | INTEREST | PENALTY | FEE | FREEZE | UNFREEZE
    private String description;
    private double amount;
    private double balanceBefore;
    private double balanceAfter;
    private double interestGain;      // non nul pour les transactions INTEREST
    private double penaltyAmount;     // non nul pour les transactions PENALTY
    private double feeApplied;        // frais de dépassement (30%) pour le compte chèque
    private Date   transactionDate;

    public TransactionDTO() {}

    public TransactionDTO(long transactionId, long accountId, int customerId,
                          String transactionType, String description,
                          double amount, double balanceBefore, double balanceAfter,
                          double interestGain, double penaltyAmount, double feeApplied,
                          Date transactionDate) {
        this.transactionId   = transactionId;
        this.accountId       = accountId;
        this.customerId      = customerId;
        this.transactionType = transactionType;
        this.description     = description;
        this.amount          = amount;
        this.balanceBefore   = balanceBefore;
        this.balanceAfter    = balanceAfter;
        this.interestGain    = interestGain;
        this.penaltyAmount   = penaltyAmount;
        this.feeApplied      = feeApplied;
        this.transactionDate = transactionDate;
    }

    public long   getTransactionId()   { return transactionId; }
    public void   setTransactionId(long v)   { this.transactionId = v; }

    public long   getAccountId()       { return accountId; }
    public void   setAccountId(long v)       { this.accountId = v; }

    public int    getCustomerId()      { return customerId; }
    public void   setCustomerId(int v)       { this.customerId = v; }

    public String getTransactionType() { return transactionType; }
    public void   setTransactionType(String v) { this.transactionType = v; }

    public String getDescription()     { return description; }
    public void   setDescription(String v)   { this.description = v; }

    public double getAmount()          { return amount; }
    public void   setAmount(double v)        { this.amount = v; }

    public double getBalanceBefore()   { return balanceBefore; }
    public void   setBalanceBefore(double v) { this.balanceBefore = v; }

    public double getBalanceAfter()    { return balanceAfter; }
    public void   setBalanceAfter(double v)  { this.balanceAfter = v; }

    public double getInterestGain()    { return interestGain; }
    public void   setInterestGain(double v)  { this.interestGain = v; }

    public double getPenaltyAmount()   { return penaltyAmount; }
    public void   setPenaltyAmount(double v) { this.penaltyAmount = v; }

    public double getFeeApplied()      { return feeApplied; }
    public void   setFeeApplied(double v)    { this.feeApplied = v; }

    public Date   getTransactionDate() { return transactionDate; }
    public void   setTransactionDate(Date v) { this.transactionDate = v; }

    @Override
    public String toString() {
        return String.format("TX[%d] Compte=%d Type=%-12s Mnt=$%.2f Solde:$%.2f->$%.2f Frais=$%.2f Date=%s",
            transactionId, accountId, transactionType, amount,
            balanceBefore, balanceAfter, feeApplied, transactionDate);
    }
}
