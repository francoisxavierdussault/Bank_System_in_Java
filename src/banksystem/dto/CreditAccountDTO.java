package banksystem.dto;

import java.util.Date;

/**
 * DTO pour CreditAccount_Details joint avec BankAccount.
 */
public class CreditAccountDTO extends AccountDTO {

    private double creditLimit;
    private double interestRate;        // ex. 0.20 = 20% annuel
    private double minPaymentPercent;   // ex. 2.0 = 2% du solde dû chaque mois
    private double paymentDueAmount;
    private Date   paymentDueDate;
    private double penaltyAmount;       // pénalité si paiement manqué
    private double totalInterestPaid;
    private double totalPenaltiesPaid;
    private Date   lastChargeDate;

    public CreditAccountDTO() { super(); }

    public CreditAccountDTO(long accountId, int customerId, double balance, String status,
                            double dailyLimit, Date openedDate,
                            double creditLimit, double interestRate, double minPaymentPercent,
                            double paymentDueAmount, Date paymentDueDate,
                            double penaltyAmount, double totalInterestPaid,
                            double totalPenaltiesPaid, Date lastChargeDate) {
        super(accountId, customerId, "CREDIT", balance, status, dailyLimit, openedDate);
        this.creditLimit        = creditLimit;
        this.interestRate       = interestRate;
        this.minPaymentPercent  = minPaymentPercent;
        this.paymentDueAmount   = paymentDueAmount;
        this.paymentDueDate     = paymentDueDate;
        this.penaltyAmount      = penaltyAmount;
        this.totalInterestPaid  = totalInterestPaid;
        this.totalPenaltiesPaid = totalPenaltiesPaid;
        this.lastChargeDate     = lastChargeDate;
    }

    public double getCreditLimit()              { return creditLimit; }
    public void   setCreditLimit(double v)      { this.creditLimit = v; }

    public double getInterestRate()             { return interestRate; }
    public void   setInterestRate(double v)     { this.interestRate = v; }

    public double getMinPaymentPercent()        { return minPaymentPercent; }
    public void   setMinPaymentPercent(double v){ this.minPaymentPercent = v; }

    public double getPaymentDueAmount()         { return paymentDueAmount; }
    public void   setPaymentDueAmount(double v) { this.paymentDueAmount = v; }

    public Date   getPaymentDueDate()           { return paymentDueDate; }
    public void   setPaymentDueDate(Date v)     { this.paymentDueDate = v; }

    public double getPenaltyAmount()            { return penaltyAmount; }
    public void   setPenaltyAmount(double v)    { this.penaltyAmount = v; }

    public double getTotalInterestPaid()        { return totalInterestPaid; }
    public void   setTotalInterestPaid(double v){ this.totalInterestPaid = v; }

    public double getTotalPenaltiesPaid()           { return totalPenaltiesPaid; }
    public void   setTotalPenaltiesPaid(double v)   { this.totalPenaltiesPaid = v; }

    public Date   getLastChargeDate()           { return lastChargeDate; }
    public void   setLastChargeDate(Date v)     { this.lastChargeDate = v; }

    /** Crédit disponible = limite − montant dû lorsque le solde est négatif. */
    public double getAvailableCredit() {
        double owed = getBalance() < 0 ? Math.abs(getBalance()) : 0;
        return creditLimit - owed;
    }

    @Override
    public String toString() {
        return String.format("CreditAccountDTO[%d] Solde=$%.2f Limite=$%.2f Dû=$%.2f Pénalité=$%.2f",
            getAccountId(), getBalance(), creditLimit, paymentDueAmount, penaltyAmount);
    }
}
