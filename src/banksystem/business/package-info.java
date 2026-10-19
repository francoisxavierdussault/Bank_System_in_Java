/**
 * Couche métier de FortisBank Systems.
 *
 * <p>Contient les entités du domaine bancaire et la logique financière associée :</p>
 * <ul>
 *   <li>{@link banksystem.business.Account} — classe abstraite de base définissant dépôt,
 *       retrait, virement et la limite journalière de retrait (5 000 $ par défaut).</li>
 *   <li>{@link banksystem.business.CheckingAccount} — compte chèque avec 3 transactions
 *       gratuites par mois et frais de 30 % au-delà.</li>
 *   <li>{@link banksystem.business.SavingsAccount} — compte épargne avec intérêts mensuels
 *       à taux configurable.</li>
 *   <li>{@link banksystem.business.CreditAccount} — compte crédit autorisant un solde négatif
 *       jusqu'à la limite de crédit, avec intérêts et pénalités de retard.</li>
 *   <li>{@link banksystem.business.CurrencyAccount} — compte devise gérant le gel de dollars
 *       USD pour une durée fixe.</li>
 *   <li>{@link banksystem.business.Customer} — représentation d'un client bancaire.</li>
 *   <li>{@link banksystem.business.Transaction} — enregistrement immuable d'une opération.</li>
 *   <li>{@link banksystem.business.Validator} — utilitaire statique de validation des données
 *       (montants, identifiants, champs obligatoires).</li>
 *   <li>{@link banksystem.business.ITransaction} — interface définissant le contrat
 *       withdraw/deposit/checkBalance/transfert.</li>
 * </ul>
 *
 * <p><strong>Dépendances :</strong> ce package ne dépend que de {@code banksystem.enums},
 * {@code banksystem.exceptions} et {@code banksystem.util.BankLogger}.</p>
 */
package banksystem.business;
