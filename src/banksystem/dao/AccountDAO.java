package banksystem.dao;

import banksystem.dto.AccountDTO;
import banksystem.util.BankLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Couche d'accès aux données pour la table BankAccount — CRUD complet via Oracle XE.
 * Gère la persistance du solde, du statut et la création de comptes.
 */
public class AccountDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "AccountDAO";

    private Connection        myConnection;
    private PreparedStatement myPreparedStatement;
    private ResultSet         myResultSet;

    public AccountDAO() {
        myConnection = DBConnection.getConnection();
    }

    // -------------------------------------------------------------------------
    // SELECT
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les comptes d'un client donné.
     *
     * @param customerId identifiant du client
     * @return liste des comptes du client
     * @implNote O(k) — k = nombre de comptes du client (généralement très faible, &lt; 10).
     */
    public List<AccountDTO> selectByCustomer(int customerId) {
        List<AccountDTO> list = new ArrayList<>();
        String sql = "SELECT Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit, Opened_Date "
                   + "FROM BankAccount WHERE Customer_ID = ? ORDER BY Account_ID";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setInt(1, customerId);
            myResultSet = myPreparedStatement.executeQuery();
            while (myResultSet.next()) {
                list.add(mapRow(myResultSet));
            }
            logger.info(SRC, "selectByCustomer(" + customerId + ") → " + list.size() + " compte(s)");
        } catch (SQLException ex) {
            logger.error(SRC, "selectByCustomer(" + customerId + ") erreur : " + ex.getMessage());
        }
        return list;
    }

    /**
     * Retourne un compte par son identifiant, ou null s'il n'existe pas.
     *
     * @param accountId identifiant du compte
     * @return AccountDTO correspondant, ou null
     * @implNote O(1) — recherche par clé primaire indexée.
     */
    public AccountDTO selectById(long accountId) {
        String sql = "SELECT Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit, Opened_Date "
                   + "FROM BankAccount WHERE Account_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setLong(1, accountId);
            myResultSet = myPreparedStatement.executeQuery();
            if (myResultSet.next()) return mapRow(myResultSet);
        } catch (SQLException ex) {
            logger.error(SRC, "selectById(" + accountId + ") erreur : " + ex.getMessage());
        }
        return null;
    }

    /**
     * Retourne tous les comptes (utilisé par la vue gestionnaire).
     *
     * @return liste de tous les comptes de la banque
     * @implNote O(n) — n = nombre total de comptes dans la table BankAccount.
     */
    public List<AccountDTO> selectAll() {
        List<AccountDTO> list = new ArrayList<>();
        String sql = "SELECT Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit, Opened_Date "
                   + "FROM BankAccount ORDER BY Customer_ID, Account_ID";
        try {
            Statement stmt = myConnection.createStatement();
            myResultSet = stmt.executeQuery(sql);
            while (myResultSet.next()) list.add(mapRow(myResultSet));
            logger.info(SRC, "selectAll() → " + list.size() + " compte(s)");
        } catch (SQLException ex) {
            logger.error(SRC, "selectAll() erreur : " + ex.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // INSERT
    // -------------------------------------------------------------------------

    /**
     * Insère un nouveau compte.
     *
     * @param dto données du compte à insérer
     * @return nombre de lignes insérées (1 si succès, 0 si échec)
     */
    public int insert(AccountDTO dto) {
        String sql = "INSERT INTO BankAccount (Account_ID, Customer_ID, Account_Type, Balance, Status, Daily_Limit) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setLong  (1, dto.getAccountId());
            myPreparedStatement.setInt   (2, dto.getCustomerId());
            myPreparedStatement.setString(3, dto.getAccountType());
            myPreparedStatement.setDouble(4, dto.getBalance());
            myPreparedStatement.setString(5, dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
            myPreparedStatement.setDouble(6, dto.getDailyLimit() > 0 ? dto.getDailyLimit() : 5000.0);
            int rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "insert() succès — Compte ID=" + dto.getAccountId()
                    + " Client=" + dto.getCustomerId() + " Type=" + dto.getAccountType());
            return rows;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() erreur pour Compte ID=" + dto.getAccountId() + " : " + ex.getMessage());
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE — mises à jour ciblées par colonne
    // -------------------------------------------------------------------------

    /**
     * Persiste le solde actuel d'un compte dans Oracle.
     * Appelé après chaque dépôt, retrait ou virement.
     *
     * @param accountId  identifiant du compte
     * @param newBalance nouveau solde
     * @return nombre de lignes modifiées
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public int updateBalance(long accountId, double newBalance) {
        String sql = "UPDATE BankAccount SET Balance = ? WHERE Account_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setDouble(1, newBalance);
            myPreparedStatement.setLong  (2, accountId);
            int rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "updateBalance Compte#" + accountId + " → $" + String.format("%.2f", newBalance));
            return rows;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "updateBalance(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Persiste le statut (ACTIVE/FROZEN/CLOSED) d'un compte dans Oracle.
     * Appelé après un gel ou dégel de compte.
     *
     * @param accountId identifiant du compte
     * @param newStatus nouveau statut
     * @return nombre de lignes modifiées
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public int updateStatus(long accountId, String newStatus) {
        String sql = "UPDATE BankAccount SET Status = ? WHERE Account_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setString(1, newStatus);
            myPreparedStatement.setLong  (2, accountId);
            int rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "updateStatus Compte#" + accountId + " → " + newStatus);
            return rows;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "updateStatus(" + accountId + ") erreur : " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Mise à jour complète de tous les champs modifiables (solde + statut + limite journalière).
     *
     * @param dto données mises à jour du compte
     * @return nombre de lignes modifiées
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public int update(AccountDTO dto) {
        String sql = "UPDATE BankAccount SET Balance = ?, Status = ?, Daily_Limit = ? WHERE Account_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setDouble(1, dto.getBalance());
            myPreparedStatement.setString(2, dto.getStatus());
            myPreparedStatement.setDouble(3, dto.getDailyLimit());
            myPreparedStatement.setLong  (4, dto.getAccountId());
            int rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "update() succès — Compte ID=" + dto.getAccountId());
            return rows;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "update() erreur pour Compte ID=" + dto.getAccountId() + " : " + ex.getMessage());
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /**
     * Supprime un compte par son identifiant.
     *
     * @param accountId identifiant du compte à supprimer
     * @return nombre de lignes supprimées
     * @implNote O(1) — suppression par clé primaire indexée.
     */
    public int delete(long accountId) {
        String sql = "DELETE FROM BankAccount WHERE Account_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(sql);
            myPreparedStatement.setLong(1, accountId);
            int rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "delete() succès — Compte ID=" + accountId);
            return rows;
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "delete() erreur pour Compte ID=" + accountId + " : " + ex.getMessage());
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Méthode privée
    // -------------------------------------------------------------------------

    private AccountDTO mapRow(ResultSet rs) throws SQLException {
        return new AccountDTO(
            rs.getLong  ("Account_ID"),
            rs.getInt   ("Customer_ID"),
            rs.getString("Account_Type"),
            rs.getDouble("Balance"),
            rs.getString("Status"),
            rs.getDouble("Daily_Limit"),
            rs.getDate  ("Opened_Date")
        );
    }
}
