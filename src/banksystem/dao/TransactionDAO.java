package banksystem.dao;

import banksystem.dto.TransactionDTO;
import banksystem.util.BankLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la table Transaction — insertion et interrogation de la piste d'audit complète.
 * La vue client retourne au maximum 3 transactions récentes par compte.
 */
public class TransactionDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "TransactionDAO";
    private static final int        CUSTOMER_VIEW_LIMIT = 3;

    private Connection myConnection;

    public TransactionDAO() {
        myConnection = DBConnection.getConnection();
    }

    // -------------------------------------------------------------------------
    // INSERT
    // -------------------------------------------------------------------------

    /**
     * Persiste une nouvelle transaction. Retourne le Transaction_ID généré, ou -1 en cas d'échec.
     *
     * @param dto données de la transaction
     * @return identifiant généré ou -1
     * @implNote O(1) — insertion d'une seule ligne via séquence Oracle.
     */
    public long insert(TransactionDTO dto) {
        String sql =
            "INSERT INTO Transaction "
            + "(Transaction_ID, Account_ID, Customer_ID, Transaction_Type, Description, "
            + " Amount, Balance_Before, Balance_After, Interest_Gain, Penalty_Amount, Fee_Applied, Transaction_Date) "
            + "VALUES (tx_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";
        try (PreparedStatement ps = myConnection.prepareStatement(sql, new String[]{"Transaction_ID"})) {
            ps.setLong  (1, dto.getAccountId());
            ps.setInt   (2, dto.getCustomerId());
            ps.setString(3, dto.getTransactionType());
            ps.setString(4, dto.getDescription());
            ps.setDouble(5, dto.getAmount());
            ps.setDouble(6, dto.getBalanceBefore());
            ps.setDouble(7, dto.getBalanceAfter());
            ps.setDouble(8, dto.getInterestGain());
            ps.setDouble(9, dto.getPenaltyAmount());
            ps.setDouble(10, dto.getFeeApplied());
            ps.executeUpdate();
            myConnection.commit();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                logger.info(SRC, "insert() TX_ID=" + id
                    + " Compte=" + dto.getAccountId() + " Type=" + dto.getTransactionType()
                    + " Mnt=$" + String.format("%.2f", dto.getAmount()));
                return id;
            }
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() erreur : " + ex.getMessage());
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // SELECT — vue client (3 transactions récentes par compte)
    // -------------------------------------------------------------------------

    /**
     * Retourne les 3 transactions les plus récentes d'un compte.
     * Utilisé pour l'affichage des mouvements dans la vue client.
     *
     * @param accountId identifiant du compte
     * @return liste des 3 dernières transactions
     * @implNote O(1) — ROWNUM &lt;= 3 borne la lecture à 3 lignes au maximum.
     */
    public List<TransactionDTO> selectRecentByAccount(long accountId) {
        return selectRecentByAccount(accountId, CUSTOMER_VIEW_LIMIT);
    }

    /**
     * Retourne au plus {@code limit} transactions récentes pour un compte.
     *
     * @param accountId identifiant du compte
     * @param limit     nombre maximum de transactions à retourner
     * @return liste des transactions
     * @implNote O(limit) — borné par le ROWNUM Oracle ; indépendant du volume total de transactions.
     */
    public List<TransactionDTO> selectRecentByAccount(long accountId, int limit) {
        List<TransactionDTO> list = new ArrayList<>();
        String sql =
            "SELECT * FROM ("
            + "  SELECT Transaction_ID, Account_ID, Customer_ID, Transaction_Type, Description, "
            + "         Amount, Balance_Before, Balance_After, Interest_Gain, Penalty_Amount, Fee_Applied, Transaction_Date "
            + "  FROM Transaction WHERE Account_ID = ? ORDER BY Transaction_Date DESC"
            + ") WHERE ROWNUM <= ?";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setInt (2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
            logger.info(SRC, "selectRecentByAccount(Compte=" + accountId + ") → " + list.size() + " tx");
        } catch (SQLException ex) {
            logger.error(SRC, "selectRecentByAccount(" + accountId + ") erreur : " + ex.getMessage());
        }
        return list;
    }

    /**
     * Retourne toutes les transactions d'un client (tous ses comptes), triées par date DESC.
     * Utilisé pour l'export complet de l'historique.
     *
     * @param customerId identifiant du client
     * @return liste complète des transactions
     * @implNote O(n) — n = nombre total de transactions du client.
     */
    public List<TransactionDTO> selectAllByCustomer(int customerId) {
        List<TransactionDTO> list = new ArrayList<>();
        String sql =
            "SELECT Transaction_ID, Account_ID, Customer_ID, Transaction_Type, Description, "
            + "Amount, Balance_Before, Balance_After, Interest_Gain, Penalty_Amount, Fee_Applied, Transaction_Date "
            + "FROM Transaction WHERE Customer_ID = ? ORDER BY Transaction_Date DESC";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
            logger.info(SRC, "selectAllByCustomer(" + customerId + ") → " + list.size() + " tx");
        } catch (SQLException ex) {
            logger.error(SRC, "selectAllByCustomer(" + customerId + ") erreur : " + ex.getMessage());
        }
        return list;
    }

    /**
     * Retourne toutes les transactions d'un compte (pour l'export PDF/CSV).
     *
     * @param accountId identifiant du compte
     * @return liste complète des transactions du compte
     * @implNote O(n) — n = nombre total de transactions du compte.
     */
    public List<TransactionDTO> selectAllByAccount(long accountId) {
        List<TransactionDTO> list = new ArrayList<>();
        String sql =
            "SELECT Transaction_ID, Account_ID, Customer_ID, Transaction_Type, Description, "
            + "Amount, Balance_Before, Balance_After, Interest_Gain, Penalty_Amount, Fee_Applied, Transaction_Date "
            + "FROM Transaction WHERE Account_ID = ? ORDER BY Transaction_Date DESC";
        try (PreparedStatement ps = myConnection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            logger.error(SRC, "selectAllByAccount(" + accountId + ") erreur : " + ex.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Méthode privée
    // -------------------------------------------------------------------------

    private TransactionDTO mapRow(ResultSet rs) throws SQLException {
        return new TransactionDTO(
            rs.getLong  ("Transaction_ID"),
            rs.getLong  ("Account_ID"),
            rs.getInt   ("Customer_ID"),
            rs.getString("Transaction_Type"),
            rs.getString("Description"),
            rs.getDouble("Amount"),
            rs.getDouble("Balance_Before"),
            rs.getDouble("Balance_After"),
            rs.getDouble("Interest_Gain"),
            rs.getDouble("Penalty_Amount"),
            rs.getDouble("Fee_Applied"),
            rs.getDate  ("Transaction_Date")
        );
    }
}
