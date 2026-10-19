package banksystem.dao;

import banksystem.dto.CurrencyAccountDTO;
import banksystem.util.BankLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour CurrencyAccount — gère le cycle de vie gel/dégel des devises USD.
 */
public class CurrencyAccountDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "CurrencyAccountDAO";

    private Connection myConnection;

    public CurrencyAccountDAO() {
        myConnection = DBConnection.getConnection();
    }

    /**
     * Retourne les détails d'un compte devise par son identifiant, ou null s'il n'existe pas.
     *
     * @param accountId identifiant du compte
     * @return le DTO ou null
     */
    public CurrencyAccountDTO selectById(long accountId) {
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "c.Currency_Code, c.Frozen_Amount_USD, c.Exchange_Rate_At_Freeze, "
            + "c.Freeze_Months, c.Freeze_Start_Date, c.Unfreeze_Date, c.Is_Frozen "
            + "FROM BankAccount b JOIN CurrencyAccount c ON c.Account_ID = b.Account_ID "
            + "WHERE b.Account_ID = ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            logger.error(SRC, "selectById(" + accountId + ") erreur : " + ex.getMessage());
        }
        return null;
    }

    /**
     * Retourne tous les comptes devise d'un client.
     *
     * @param customerId identifiant du client
     * @return liste des comptes devise
     */
    public List<CurrencyAccountDTO> selectByCustomer(int customerId) {
        List<CurrencyAccountDTO> list = new ArrayList<>();
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "c.Currency_Code, c.Frozen_Amount_USD, c.Exchange_Rate_At_Freeze, "
            + "c.Freeze_Months, c.Freeze_Start_Date, c.Unfreeze_Date, c.Is_Frozen "
            + "FROM BankAccount b JOIN CurrencyAccount c ON c.Account_ID = b.Account_ID "
            + "WHERE b.Customer_ID = ? ORDER BY b.Account_ID";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            logger.error(SRC, "selectByCustomer(" + customerId + ") erreur : " + ex.getMessage());
        }
        return list;
    }

    /**
     * Insère une ligne dans BankAccount et une ligne dans CurrencyAccount.
     *
     * @param dto données du compte devise
     * @return 1 si succès, 0 si échec
     */
    public int insert(CurrencyAccountDTO dto) {
        String sqlAcc =
            "INSERT INTO BankAccount (Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit) "
            + "VALUES (?, ?, 'CURRENCY', ?, ?, ?)";
        String sqlDet =
            "INSERT INTO CurrencyAccount "
            + "(Account_ID, Currency_Code, Frozen_Amount_USD, Exchange_Rate_At_Freeze, "
            + " Freeze_Months, Is_Frozen) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps1 = myConnection.prepareStatement(sqlAcc);
            ps1.setLong  (1, dto.getAccountId());
            ps1.setInt   (2, dto.getCustomerId());
            ps1.setDouble(3, dto.getBalance());
            ps1.setString(4, dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
            ps1.setDouble(5, dto.getDailyLimit() > 0 ? dto.getDailyLimit() : 5000.0);
            ps1.executeUpdate();

            PreparedStatement ps2 = myConnection.prepareStatement(sqlDet);
            ps2.setLong  (1, dto.getAccountId());
            ps2.setString(2, dto.getCurrencyCode() != null ? dto.getCurrencyCode() : "USD");
            ps2.setDouble(3, dto.getFrozenAmountUSD());
            ps2.setDouble(4, dto.getExchangeRateAtFreeze());
            ps2.setInt   (5, dto.getFreezeMonths());
            ps2.setInt   (6, dto.isFrozen() ? 1 : 0);
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "insert() CurrencyAccount ID=" + dto.getAccountId());
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Persiste l'opération de gel : montant USD, taux de change, dates, Is_Frozen=1.
     *
     * @param accountId      identifiant du compte
     * @param newBalanceCAD  nouveau solde CAD après déduction du montant gelé
     * @param frozenUSD      montant gelé en USD
     * @param exchangeRate   taux de change CAD/USD au moment du gel
     * @param months         durée du gel en mois
     * @param freezeStart    date de début du gel
     * @param unfreezeDate   date de fin du gel
     * @return 1 si succès, 0 si échec
     */
    public int freeze(long accountId, double newBalanceCAD, double frozenUSD,
                      double exchangeRate, int months,
                      java.util.Date freezeStart, java.util.Date unfreezeDate) {
        String sql =
            "UPDATE CurrencyAccount "
            + "SET Frozen_Amount_USD = ?, Exchange_Rate_At_Freeze = ?, Freeze_Months = ?, "
            + "    Freeze_Start_Date = ?, Unfreeze_Date = ?, Is_Frozen = 1 "
            + "WHERE Account_ID = ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setDouble(1, frozenUSD);
            ps.setDouble(2, exchangeRate);
            ps.setInt   (3, months);
            ps.setDate  (4, new java.sql.Date(freezeStart.getTime()));
            ps.setDate  (5, new java.sql.Date(unfreezeDate.getTime()));
            ps.setLong  (6, accountId);
            ps.executeUpdate();

            PreparedStatement ps2 = myConnection.prepareStatement(
                "UPDATE BankAccount SET Balance = ? WHERE Account_ID = ?");
            ps2.setDouble(1, newBalanceCAD);
            ps2.setLong  (2, accountId);
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "freeze() Compte=" + accountId
                + " $" + String.format("%.2f", frozenUSD) + " USD jusqu'au " + unfreezeDate);
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "freeze(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Persiste le dégel : réinitialise le montant gelé et passe Is_Frozen=0.
     *
     * @param accountId      identifiant du compte
     * @param newBalanceCAD  nouveau solde CAD après récupération des fonds
     * @return 1 si succès, 0 si échec
     */
    public int unfreeze(long accountId, double newBalanceCAD) {
        String sql =
            "UPDATE CurrencyAccount "
            + "SET Frozen_Amount_USD = 0, Is_Frozen = 0 WHERE Account_ID = ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.executeUpdate();

            PreparedStatement ps2 = myConnection.prepareStatement(
                "UPDATE BankAccount SET Balance = ? WHERE Account_ID = ?");
            ps2.setDouble(1, newBalanceCAD);
            ps2.setLong  (2, accountId);
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "unfreeze() Compte=" + accountId
                + " NouveauSolde=$" + String.format("%.2f", newBalanceCAD) + " CAD");
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "unfreeze(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    private CurrencyAccountDTO mapRow(ResultSet rs) throws SQLException {
        return new CurrencyAccountDTO(
            rs.getLong  ("Account_ID"),
            rs.getInt   ("Customer_ID"),
            rs.getDouble("Balance"),
            rs.getString("Status"),
            rs.getDouble("Daily_Limit"),
            rs.getDate  ("Opened_Date"),
            rs.getString("Currency_Code"),
            rs.getDouble("Frozen_Amount_USD"),
            rs.getDouble("Exchange_Rate_At_Freeze"),
            rs.getInt   ("Freeze_Months"),
            rs.getDate  ("Freeze_Start_Date"),
            rs.getDate  ("Unfreeze_Date"),
            rs.getInt   ("Is_Frozen") == 1
        );
    }
}
