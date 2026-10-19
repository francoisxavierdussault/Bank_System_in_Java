/**
 * Couche d'accès aux données (DAO) de FortisBank Systems.
 *
 * <p>Chaque classe DAO encapsule les requêtes SQL Oracle XE pour une table ou
 * un groupe de tables liées. Toutes utilisent {@link banksystem.dao.DBConnection}
 * pour obtenir la connexion partagée (auto-commit désactivé).</p>
 *
 * <p>Contenu du package :</p>
 * <ul>
 *   <li>{@link banksystem.dao.DBConnection} — singleton de connexion Oracle XE.</li>
 *   <li>{@link banksystem.dao.CustomerDAO} — CRUD sur la table {@code Customer},
 *       avec recherche par nom (modes PREFIX / SUFFIX / CONTAINS).</li>
 *   <li>{@link banksystem.dao.ManagerDAO} — CRUD sur la table {@code Manager}.</li>
 *   <li>{@link banksystem.dao.AccountDAO} — CRUD sur la table {@code BankAccount}
 *       (solde, statut, limite journalière).</li>
 *   <li>{@link banksystem.dao.CheckingAccountDAO} — jointure
 *       {@code BankAccount ⋈ CheckingAccount_Details}.</li>
 *   <li>{@link banksystem.dao.SavingsAccountDAO} — jointure
 *       {@code BankAccount ⋈ SavingsAccount_Details}.</li>
 *   <li>{@link banksystem.dao.CreditAccountDAO} — jointure
 *       {@code BankAccount ⋈ CreditAccount_Details}.</li>
 *   <li>{@link banksystem.dao.CurrencyAccountDAO} — jointure
 *       {@code BankAccount ⋈ CurrencyAccount} avec opérations freeze/unfreeze.</li>
 *   <li>{@link banksystem.dao.TransactionDAO} — insertion et interrogation de la
 *       piste d'audit (3 dernières TX par défaut pour la vue client).</li>
 *   <li>{@link banksystem.dao.LoginDAO} — lecture/écriture des colonnes
 *       {@code Pwd_Hash}, {@code Failed_Login} et {@code Is_Locked}.</li>
 * </ul>
 *
 * <p><strong>Convention de gestion des erreurs :</strong> chaque méthode attrape
 * {@code SQLException}, journalise via {@link banksystem.util.BankLogger} et retourne
 * {@code null} / {@code 0} / liste vide selon le type de retour attendu.</p>
 */
package banksystem.dao;
