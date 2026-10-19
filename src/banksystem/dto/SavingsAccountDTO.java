package banksystem.dto;

import java.util.Date;

/**
 * DTO pour SavingsAccount_Details joint avec BankAccount.
 */
public class SavingsAccountDTO extends AccountDTO {

    private double interestRate;
    private Date   lastInterestDate;
    private double monthlyGain;
    private double totalGain;

    public SavingsAccountDTO() { super(); }

    public SavingsAccountDTO(long accountId, int customerId, double balance, String status,
                             double dailyLimit, Date openedDate,
                             double interestRate, Date lastInterestDate,
                             double monthlyGain, double totalGain) {
        super(accountId, customerId, "SAVINGS", balance, status, dailyLimit, openedDate);
        this.interestRate     = interestRate;
        this.lastInterestDate = lastInterestDate;
        this.monthlyGain      = monthlyGain;
        this.totalGain        = totalGain;
    }

    public double getInterestRate()           { return interestRate; }
    public void   setInterestRate(double v)   { this.interestRate = v; }

    public Date   getLastInterestDate()       { return lastInterestDate; }
    public void   setLastInterestDate(Date v) { this.lastInterestDate = v; }

    public double getMonthlyGain()            { return monthlyGain; }
    public void   setMonthlyGain(double v)    { this.monthlyGain = v; }

    public double getTotalGain()              { return totalGain; }
    public void   setTotalGain(double v)      { this.totalGain = v; }

    @Override
    public String toString() {
        return String.format("SavingsAccountDTO[%d] Solde=$%.2f Taux=%.2f%% GainMensuel=$%.2f GainTotal=$%.2f",
            getAccountId(), getBalance(), interestRate * 100, monthlyGain, totalGain);
    }
}
