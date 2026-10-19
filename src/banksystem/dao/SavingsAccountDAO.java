package banksystem.dao;

import banksystem.dto.SavingsAccountDTO;
import banksystem.util.BankLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour SavingsAccount_Details — gère la persistance du taux d'intérêt et des gains mensuels.
 */
public class SavingsAccountDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "SavingsAccountDAO";

    private Connection myConnection;

    public SavingsAccountDAO() {
        myConnection = DBConnection.getConnection();
    }

    /**
     * Retourne les détails d'un compte épargne par son identifiant, ou null s'il n'existe pas.
     *
     * @param accountId identifiant du compte
     * @return le DTO ou null
     */
    public SavingsAccountDTO selectById(long accountId) {
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "s.Interest_Rate, s.Last_Interest_Date, s.Monthly_Gain, s.Total_Gain "
            + "FROM BankAccount b JOIN SavingsAccount_Details s ON s.Account_ID = b.Account_ID "
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
     * Retourne tous les comptes épargne d'un client.
     *
     * @param customerId identifiant du client
     * @return liste des comptes épargne
     */
    public List<SavingsAccountDTO> selectByCustomer(int customerId) {
        List<SavingsAccountDTO> list = new ArrayList<>();
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "s.Interest_Rate, s.Last_Interest_Date, s.Monthly_Gain, s.Total_Gain "
            + "FROM BankAccount b JOIN SavingsAccount_Details s ON s.Account_ID = b.Account_ID "
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
     * Insère une ligne dans BankAccount et une ligne dans SavingsAccount_Details.
     *
     * @param dto données du compte épargne
     * @return 1 si succès, 0 si échec
     */
    public int insert(SavingsAccountDTO dto) {
        String sqlAcc =
            "INSERT INTO BankAccount (Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit) "
            + "VALUES (?, ?, 'SAVINGS', ?, ?, ?)";
        String sqlDet =
            "INSERT INTO SavingsAccount_Details (Account_ID, Interest_Rate, Monthly_Gain, Total_Gain) "
            + "VALUES (?, ?, ?, ?)";
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
            ps2.setDouble(2, dto.getInterestRate());
            ps2.setDouble(3, dto.getMonthlyGain());
            ps2.setDouble(4, dto.getTotalGain());
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "insert() SavingsAccount ID=" + dto.getAccountId());
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Met à jour les statistiques d'intérêt (gain mensuel, gain total, date du dernier intérêt).
     *
     * @param accountId    identifiant du compte
     * @param newBalance   nouveau solde après application des intérêts
     * @param monthlyGain  gain accumulé ce mois-ci
     * @param totalGain    gain cumulé depuis l'ouverture
     * @return 1 si succès, 0 si échec
     */
    public int updateGains(long accountId, double newBalance, double monthlyGain, double totalGain) {
        String sql =
            "UPDATE SavingsAccount_Details "
            + "SET Monthly_Gain = ?, Total_Gain = ?, Last_Interest_Date = SYSDATE "
            + "WHERE Account_ID = ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setDouble(1, monthlyGain);
            ps.setDouble(2, totalGain);
            ps.setLong  (3, accountId);
            ps.executeUpdate();

            PreparedStatement ps2 = myConnection.prepareStatement(
                "UPDATE BankAccount SET Balance = ? WHERE Account_ID = ?");
            ps2.setDouble(1, newBalance);
            ps2.setLong  (2, accountId);
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "updateGains Compte=" + accountId
                + " GainMensuel=$" + String.format("%.2f", monthlyGain)
                + " GainTotal=$" + String.format("%.2f", totalGain));
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "updateGains(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    private SavingsAccountDTO mapRow(ResultSet rs) throws SQLException {
        return new SavingsAccountDTO(
            rs.getLong  ("Account_ID"),
            rs.getInt   ("Customer_ID"),
            rs.getDouble("Balance"),
            rs.getString("Status"),
            rs.getDouble("Daily_Limit"),
            rs.getDate  ("Opened_Date"),
            rs.getDouble("Interest_Rate"),
            rs.getDate  ("Last_Interest_Date"),
            rs.getDouble("Monthly_Gain"),
            rs.getDouble("Total_Gain")
        );
    }
}
