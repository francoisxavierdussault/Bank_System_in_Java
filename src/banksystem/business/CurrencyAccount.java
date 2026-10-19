package banksystem.business;

import banksystem.enums.EnumAccountType;
import banksystem.enums.EnumTransactionType;
import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import banksystem.util.BankLogger;

import java.util.Calendar;
import java.util.Date;

/**
 * Compte devise — le client gèle des dollars USD durant X mois.
 * Le retrait n'est autorisé qu'après la date de dégel.
 */
public class CurrencyAccount extends Account {

    private static final BankLogger logger = BankLogger.getInstance();

    private String currencyCode;          // toujours "USD"
    private double frozenAmountUSD;
    private double exchangeRateAtFreeze;  // taux CAD/USD au moment du gel
    private int    freezeMonths;
    private Date   freezeStartDate;
    private Date   unfreezeDate;
    private boolean frozen;

    public CurrencyAccount() {
        super();
        this.currencyCode = "USD";
        this.frozen = false;
    }

    public CurrencyAccount(long accountNumber, Long customerNumber, double balanceCAD)
            throws NotInRangeException, NotNullException {
        super(accountNumber, customerNumber, balanceCAD, EnumAccountType.CURRENCY);
        this.currencyCode = "USD";
        this.frozen = false;
    }

    // -------------------------------------------------------------------------
    // Gel / Dégel
    // -------------------------------------------------------------------------

    /**
     * Gèle le montant CAD spécifié, le convertit en USD au taux donné,
     * et bloque le compte jusqu'à la date de dégel.
     *
     * @param amountCAD    montant en CAD à convertir et geler
     * @param exchangeRate taux CAD/USD (ex. 1.36 = 1 USD coûte 1.36 CAD)
     * @param months       durée du gel en mois
     * @implNote O(1) — calcul de conversion, calcul de date et enregistrement d'une transaction.
     */
    public void freezeFunds(double amountCAD, double exchangeRate, int months)
            throws InsufficientFundException, NotInRangeException {
        checkActive("Freeze");
        if (frozen) throw new InsufficientFundException("Des fonds sont déjà gelés sur ce compte.");
        if (amountCAD <= 0) throw new NotInRangeException("Le montant à geler doit être supérieur à 0.");
        if (exchangeRate <= 0) throw new NotInRangeException("Le taux de change doit être supérieur à 0.");
        if (months <= 0) throw new NotInRangeException("La durée du gel doit être d'au moins 1 mois.");

        double balanceBefore = getBalance();
        if (amountCAD > balanceBefore)
            throw new InsufficientFundException(
                "Solde insuffisant. Disponible : $" + String.format("%.2f", balanceBefore) + " CAD");

        this.frozenAmountUSD      = amountCAD / exchangeRate;
        this.exchangeRateAtFreeze = exchangeRate;
        this.freezeMonths         = months;
        this.freezeStartDate      = new Date();
        this.frozen               = true;

        // Calcul de la date de dégel
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.freezeStartDate);
        cal.add(Calendar.MONTH, months);
        this.unfreezeDate = cal.getTime();

        // Déduction du solde CAD (converti en USD gelé)
        this.balance -= amountCAD;

        try {
            Transaction t = new Transaction(
                getTransactions().size() + 1,
                String.format("Gel $%.2f CAD -> $%.2f USD pour %d mois jusqu'au %s",
                    amountCAD, frozenAmountUSD, months, unfreezeDate),
                amountCAD, EnumTransactionType.TRANSFER);
            getTransactions().add(t);
        } catch (Exception e) {
            logger.error("CurrencyAccount#" + getAccountNumber(), "Erreur enregistrement gel : " + e.getMessage());
        }

        logger.info("CurrencyAccount#" + getAccountNumber(),
            String.format("GEL $%.2f CAD -> $%.2f USD | Taux=%.4f | Jusqu'au %s",
                amountCAD, frozenAmountUSD, exchangeRate, unfreezeDate));
    }

    /**
     * Retire les fonds USD gelés (reconvertis en CAD au taux courant)
     * après la fin de la période de gel.
     *
     * @param currentExchangeRate taux CAD/USD courant
     * @return montant CAD crédité au solde
     * @implNote O(1) — reconversion + enregistrement d'une transaction.
     */
    public double withdrawFrozenFunds(double currentExchangeRate)
            throws InsufficientFundException, NotInRangeException {
        checkActive("Dégel");
        if (!frozen) throw new InsufficientFundException("Aucun fonds gelé sur ce compte.");
        if (currentExchangeRate <= 0) throw new NotInRangeException("Le taux de change doit être supérieur à 0.");
        if (!canWithdrawNow())
            throw new InsufficientFundException(
                "Les fonds sont encore gelés jusqu'au " + unfreezeDate + ".");

        double cadAmount = frozenAmountUSD * currentExchangeRate;
        double balanceBefore = getBalance();
        this.balance += cadAmount;
        this.frozen = false;

        try {
            Transaction t = new Transaction(
                getTransactions().size() + 1,
                String.format("Dégel $%.2f USD -> $%.2f CAD | Taux=%.4f", frozenAmountUSD, cadAmount, currentExchangeRate),
                cadAmount, EnumTransactionType.DEPOSIT);
            getTransactions().add(t);
        } catch (Exception e) {
            logger.error("CurrencyAccount#" + getAccountNumber(), "Erreur enregistrement dégel : " + e.getMessage());
        }

        logger.info("CurrencyAccount#" + getAccountNumber(),
            String.format("DÉGEL $%.2f USD -> $%.2f CAD | Taux=%.4f | Solde : $%.2f -> $%.2f",
                frozenAmountUSD, cadAmount, currentExchangeRate, balanceBefore, this.balance));

        double returned = cadAmount;
        this.frozenAmountUSD = 0;
        return returned;
    }

    /** Retourne true si la période de gel est terminée et que le retrait est permis. */
    public boolean canWithdrawNow() {
        if (!frozen) return true;
        return unfreezeDate != null && new Date().after(unfreezeDate);
    }

    // -------------------------------------------------------------------------
    // Override dépôt / retrait — respect de l'état de gel
    // -------------------------------------------------------------------------

    @Override
    public void deposit(double amount) throws InsufficientFundException, NotInRangeException {
        super.deposit(amount);
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundException, NotInRangeException {
        if (frozen && !canWithdrawNow())
            throw new InsufficientFundException(
                "Retrait impossible : fonds gelés jusqu'au " + unfreezeDate);
        super.withdraw(amount);
    }

    // -------------------------------------------------------------------------
    // Accesseurs / Mutateurs
    // -------------------------------------------------------------------------

    public String  getCurrencyCode()              { return currencyCode; }
    public double  getFrozenAmountUSD()           { return frozenAmountUSD; }
    public void    setFrozenAmountUSD(double v)   { this.frozenAmountUSD = v; }
    public double  getExchangeRateAtFreeze()      { return exchangeRateAtFreeze; }
    public void    setExchangeRateAtFreeze(double v) { this.exchangeRateAtFreeze = v; }
    public int     getFreezeMonths()              { return freezeMonths; }
    public void    setFreezeMonths(int v)         { this.freezeMonths = v; }
    public Date    getFreezeStartDate()           { return freezeStartDate; }
    public void    setFreezeStartDate(Date v)     { this.freezeStartDate = v; }
    public Date    getUnfreezeDate()              { return unfreezeDate; }
    public void    setUnfreezeDate(Date v)        { this.unfreezeDate = v; }
    public boolean isFrozenCurrency()             { return frozen; }
    public void    setFrozenCurrency(boolean v)   { this.frozen = v; }

    @Override
    public String toString() {
        return "CurrencyAccount [#" + getAccountNumber() + "]"
            + " Balance: $" + String.format("%.2f", getBalance()) + " CAD"
            + " | Frozen: $" + String.format("%.2f", frozenAmountUSD) + " USD"
            + " | Until: " + (unfreezeDate != null ? unfreezeDate : "N/A")
            + " | CanWithdraw: " + canWithdrawNow()
            + " | Customer: " + getCustomerNumber();
    }
}
