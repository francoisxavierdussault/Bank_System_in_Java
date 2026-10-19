package banksystem.dao;

import banksystem.dto.CreditAccountDTO;
import banksystem.util.BankLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour CreditAccount_Details — gère la limite de crédit, les intérêts, le paiement dû et les pénalités.
 */
public class CreditAccountDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "CreditAccountDAO";

    private Connection myConnection;

    public CreditAccountDAO() {
        myConnection = DBConnection.getConnection();
    }

    /**
     * Retourne les détails d'un compte crédit par son identifiant, ou null s'il n'existe pas.
     *
     * @param accountId identifiant du compte
     * @return le DTO ou null
     */
    public CreditAccountDTO selectById(long accountId) {
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "c.Credit_Limit, c.Interest_Rate, c.Min_Payment_Percent, "
            + "c.Payment_Due_Amount, c.Payment_Due_Date, c.Penalty_Amount, "
            + "c.Total_Interest_Paid, c.Total_Penalties_Paid, c.Last_Charge_Date "
            + "FROM BankAccount b JOIN CreditAccount_Details c ON c.Account_ID = b.Account_ID "
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
     * Retourne tous les comptes crédit d'un client.
     *
     * @param customerId identifiant du client
     * @return liste des comptes crédit
     */
    public List<CreditAccountDTO> selectByCustomer(int customerId) {
        List<CreditAccountDTO> list = new ArrayList<>();
        String sql =
            "SELECT b.Account_ID, b.Customer_ID, b.Balance, b.Status, b.Daily_Limit, b.Opened_Date, "
            + "c.Credit_Limit, c.Interest_Rate, c.Min_Payment_Percent, "
            + "c.Payment_Due_Amount, c.Payment_Due_Date, c.Penalty_Amount, "
            + "c.Total_Interest_Paid, c.Total_Penalties_Paid, c.Last_Charge_Date "
            + "FROM BankAccount b JOIN CreditAccount_Details c ON c.Account_ID = b.Account_ID "
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
     * Insère une ligne dans BankAccount et une ligne dans CreditAccount_Details.
     *
     * @param dto données du compte crédit
     * @return 1 si succès, 0 si échec
     */
    public int insert(CreditAccountDTO dto) {
        String sqlAcc =
            "INSERT INTO BankAccount (Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit) "
            + "VALUES (?, ?, 'CREDIT', ?, ?, ?)";
        String sqlDet =
            "INSERT INTO CreditAccount_Details "
            + "(Account_ID, Credit_Limit, Interest_Rate, Min_Payment_Percent, "
            + " Payment_Due_Amount, Penalty_Amount) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
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
            ps2.setDouble(2, dto.getCreditLimit());
            ps2.setDouble(3, dto.getInterestRate());
            ps2.setDouble(4, dto.getMinPaymentPercent());
            ps2.setDouble(5, dto.getPaymentDueAmount());
            ps2.setDouble(6, dto.getPenaltyAmount());
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "insert() CreditAccount ID=" + dto.getAccountId());
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Met à jour le paiement dû, la pénalité et les totaux cumulés après les charges mensuelles.
     *
     * @param accountId           identifiant du compte
     * @param newBalance          nouveau solde
     * @param paymentDueAmount    montant dû ce cycle
     * @param paymentDueDate      date limite de paiement
     * @param penaltyAmount       pénalité appliquée
     * @param totalInterestPaid   total des intérêts payés depuis l'ouverture
     * @param totalPenaltiesPaid  total des pénalités payées depuis l'ouverture
     * @return 1 si succès, 0 si échec
     */
    public int updateAfterMonthlyCharge(long accountId, double newBalance,
                                        double paymentDueAmount, Date paymentDueDate,
                                        double penaltyAmount,
                                        double totalInterestPaid, double totalPenaltiesPaid) {
        String sql =
            "UPDATE CreditAccount_Details "
            + "SET Payment_Due_Amount = ?, Payment_Due_Date = ?, Penalty_Amount = ?, "
            + "    Total_Interest_Paid = ?, Total_Penalties_Paid = ?, Last_Charge_Date = SYSDATE "
            + "WHERE Account_ID = ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setDouble(1, paymentDueAmount);
            ps.setDate  (2, new java.sql.Date(paymentDueDate.getTime()));
            ps.setDouble(3, penaltyAmount);
            ps.setDouble(4, totalInterestPaid);
            ps.setDouble(5, totalPenaltiesPaid);
            ps.setLong  (6, accountId);
            ps.executeUpdate();

            PreparedStatement ps2 = myConnection.prepareStatement(
                "UPDATE BankAccount SET Balance = ? WHERE Account_ID = ?");
            ps2.setDouble(1, newBalance);
            ps2.setLong  (2, accountId);
            ps2.executeUpdate();

            myConnection.commit();
            logger.info(SRC, "updateAfterMonthlyCharge Compte=" + accountId
                + " Dû=$" + String.format("%.2f", paymentDueAmount)
                + " Pénalité=$" + String.format("%.2f", penaltyAmount));
            return 1;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "updateAfterMonthlyCharge(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    private CreditAccountDTO mapRow(ResultSet rs) throws SQLException {
        return new CreditAccountDTO(
            rs.getLong  ("Account_ID"),
            rs.getInt   ("Customer_ID"),
            rs.getDouble("Balance"),
            rs.getString("Status"),
            rs.getDouble("Daily_Limit"),
            rs.getDate  ("Opened_Date"),
            rs.getDouble("Credit_Limit"),
            rs.getDouble("Interest_Rate"),
            rs.getDouble("Min_Payment_Percent"),
            rs.getDouble("Payment_Due_Amount"),
            rs.getDate  ("Payment_Due_Date"),
            rs.getDouble("Penalty_Amount"),
            rs.getDouble("Total_Interest_Paid"),
            rs.getDouble("Total_Penalties_Paid"),
            rs.getDate  ("Last_Charge_Date")
        );
    }
}
