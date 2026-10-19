package banksystem.dto;

import java.util.Date;

/**
 * DTO pour CurrencyAccount joint avec BankAccount.
 * Les dollars américains sont gelés pour X mois ; le retrait est possible uniquement après Unfreeze_Date.
 */
public class CurrencyAccountDTO extends AccountDTO {

    private String  currencyCode;          // toujours "USD"
    private double  frozenAmountUSD;       // montant verrouillé en USD
    private double  exchangeRateAtFreeze;  // CAD par USD au moment du gel
    private int     freezeMonths;          // durée choisie par le client
    private Date    freezeStartDate;
    private Date    unfreezeDate;          // freezeStartDate + freezeMonths
    private boolean frozen;               // true = verrouillé, false = retrait possible

    public CurrencyAccountDTO() { super(); }

    public CurrencyAccountDTO(long accountId, int customerId, double balance, String status,
                              double dailyLimit, Date openedDate,
                              String currencyCode, double frozenAmountUSD,
                              double exchangeRateAtFreeze, int freezeMonths,
                              Date freezeStartDate, Date unfreezeDate, boolean frozen) {
        super(accountId, customerId, "CURRENCY", balance, status, dailyLimit, openedDate);
        this.currencyCode         = currencyCode;
        this.frozenAmountUSD      = frozenAmountUSD;
        this.exchangeRateAtFreeze = exchangeRateAtFreeze;
        this.freezeMonths         = freezeMonths;
        this.freezeStartDate      = freezeStartDate;
        this.unfreezeDate         = unfreezeDate;
        this.frozen               = frozen;
    }

    public String  getCurrencyCode()              { return currencyCode; }
    public void    setCurrencyCode(String v)      { this.currencyCode = v; }

    public double  getFrozenAmountUSD()           { return frozenAmountUSD; }
    public void    setFrozenAmountUSD(double v)   { this.frozenAmountUSD = v; }

    public double  getExchangeRateAtFreeze()            { return exchangeRateAtFreeze; }
    public void    setExchangeRateAtFreeze(double v)    { this.exchangeRateAtFreeze = v; }

    public int     getFreezeMonths()              { return freezeMonths; }
    public void    setFreezeMonths(int v)         { this.freezeMonths = v; }

    public Date    getFreezeStartDate()           { return freezeStartDate; }
    public void    setFreezeStartDate(Date v)     { this.freezeStartDate = v; }

    public Date    getUnfreezeDate()              { return unfreezeDate; }
    public void    setUnfreezeDate(Date v)        { this.unfreezeDate = v; }

    public boolean isFrozen()                     { return frozen; }
    public void    setFrozen(boolean v)           { this.frozen = v; }

    /** Valeur équivalente en CAD au moment du gel. */
    public double getFrozenAmountCAD() {
        return frozenAmountUSD * exchangeRateAtFreeze;
    }

    /** Retourne true si la date de dégel est passée et le retrait est maintenant autorisé. */
    public boolean canWithdrawNow() {
        if (!frozen) return true;
        return unfreezeDate != null && new Date().after(unfreezeDate);
    }

    @Override
    public String toString() {
        return String.format("CurrencyAccountDTO[%d] USD=$%.2f GelJusquAu=%s Gelé=%s RetraitPossible=%s",
            getAccountId(), frozenAmountUSD, unfreezeDate, frozen, canWithdrawNow());
    }
}
