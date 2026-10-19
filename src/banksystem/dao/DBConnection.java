package banksystem.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gère la connexion unique à la base de données Oracle XE.
 * La connexion est partagée par tous les DAOs de l'application.
 */
public class DBConnection {

	/** URL de connexion à Oracle XE. */
	static String url  = "jdbc:h2:mem:bankdemo;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=TRUE";

	/** Nom d'utilisateur Oracle. */
	static String user = "sa";

	/** Mot de passe Oracle. */
	static String pass = "";

	/** Connexion partagée (singleton). */
	static Connection myConnection = null;

	/**
	 * Constructeur privé — classe utilitaire, ne doit pas être instanciée.
	 */
	private DBConnection() {}

	/**
	 * Retourne la connexion active. La crée si elle n'existe pas encore.
	 *
	 * @return la connexion à la base de données, ou null si la connexion échoue
	 */
	public static Connection getConnection() {
		try {
			if (myConnection == null || myConnection.isClosed()) {
				myConnection = DriverManager.getConnection(url, user, pass);
				myConnection.setAutoCommit(false);
				DemoSeeder.init(myConnection);
				System.out.println("\n\t\t Connexion réussie à Oracle XE - c##banksystem\n");
			}
		} catch (SQLException ex) {
			System.out.println("\n\t\t Connexion échouée - " + ex.getMessage() + "\n");
			System.out.println("Code d'erreur = " + ex.getErrorCode());
		}
		return myConnection;
	}
}
