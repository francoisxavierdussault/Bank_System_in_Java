package banksystem.dao;

import banksystem.dto.CheckingAccountDTO;
import banksystem.util.BankLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour CheckingAccount_Details — gère les compteurs de transactions et les frais mensuels.
 */
public class CheckingAccountDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "CheckingAccountDAO";

    private Connection myConnection;

    public CheckingAccountDAO() {
        myConnection = DBConnection.getConnection();
    }

    /**
     * Retourne les détails d'un compte chèque par son identifiant, ou null s'il n'existe pas.
     *
     * @param accountId identifiant du compte
     * @return le DTO ou null
     */
    public CheckingAccountDTO selectById(long accountId) {
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "c.Monthly_Free_TX, c.TX_Fee_Percent, c.TX_Used_This_Month, c.Last_Reset_Month "
            + "FROM BankAccount b JOIN CheckingAccount_Details c ON c.Account_ID = b.Account_ID "
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
     * Retourne tous les comptes chèque d'un client.
     *
     * @param customerId identifiant du client
     * @return liste des comptes chèque
     */
    public List<CheckingAccountDTO> selectByCustomer(int customerId) {
        List<CheckingAccountDTO> list = new ArrayList<>();
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "c.Monthly_Free_TX, c.TX_Fee_Percent, c.TX_Used_This_Month, c.Last_Reset_Month "
            + "FROM BankAccount b JOIN CheckingAccount_Details c ON c.Account_ID = b.Account_ID "
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
     * Insère une ligne dans BankAccount et une ligne dans CheckingAccount_Details.
     *
     * @param dto données du compte chèque
     * @return 1 si succès, 0 si échec
     */
    public int insert(CheckingAccountDTO dto) {
        String sqlAcc =
            "INSERT INTO BankAccount (Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit) "
            + "VALUES (?, ?, 'CHECKING', ?, ?, ?)";
        String sqlDet =
            "INSERT INTO CheckingAccount_Details "
            + "(Account_ID, Monthly_Free_TX, TX_Fee_Percent, TX_Used_This_Month, Last_Reset_Month) "
            + "VALUES (?, ?, ?, ?, ?)";
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
            ps2.setInt   (2, dto.getMonthlyFreeTX());
            ps2.setDouble(3, dto.getTxFeePercent());
            ps2.setInt   (4, dto.getTxUsedThisMonth());
            ps2.setString(5, dto.getLastResetMonth());
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "insert() CheckingAccount ID=" + dto.getAccountId());
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Met à jour le compteur de transactions du mois et le solde après une opération.
     *
     * @param accountId        identifiant du compte
     * @param newBalance       nouveau solde
     * @param txUsedThisMonth  nombre de transactions utilisées ce mois-ci
     * @param currentMonth     mois courant (format YYYY-MM)
     * @return 1 si succès, 0 si échec
     */
    public int updateTxCounter(long accountId, double newBalance, int txUsedThisMonth, String currentMonth) {
        String sql =
            "UPDATE CheckingAccount_Details "
            + "SET TX_Used_This_Month = ?, Last_Reset_Month = ? WHERE Account_ID = ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setInt   (1, txUsedThisMonth);
            ps.setString(2, currentMonth);
            ps.setLong  (3, accountId);
            ps.executeUpdate();

            PreparedStatement ps2 = myConnection.prepareStatement(
                "UPDATE BankAccount SET Balance = ? WHERE Account_ID = ?");
            ps2.setDouble(1, newBalance);
            ps2.setLong  (2, accountId);
            ps2.executeUpdate();

            myConnection.commit();
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "updateTxCounter(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    private CheckingAccountDTO mapRow(ResultSet rs) throws SQLException {
        return new CheckingAccountDTO(
            rs.getLong  ("Account_ID"),
            rs.getInt   ("Customer_ID"),
            rs.getDouble("Balance"),
            rs.getString("Status"),
            rs.getDouble("Daily_Limit"),
            rs.getDate  ("Opened_Date"),
            rs.getInt   ("Monthly_Free_TX"),
            rs.getDouble("TX_Fee_Percent"),
            rs.getInt   ("TX_Used_This_Month"),
            rs.getString("Last_Reset_Month")
        );
    }
}
