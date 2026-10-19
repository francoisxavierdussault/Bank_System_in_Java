# Bank System in Java

Application bancaire de bureau développée en **Java (Swing)**, organisée selon une architecture en couches (UI → Service → DAO → Base de données).

## Fonctionnalités

- **Authentification** : fenêtre de connexion avec gestion des rôles (Client / Gestionnaire)
  - Verrouillage du compte après 3 échecs de connexion
  - Mot de passe par défaut à la première connexion (ID numérique), modification obligatoire
  - Journalisation des sessions
- **Espace Gestionnaire** (`BankManager`) : gestion des comptes et des clients
- **Espace Client** (`BankCustomer`) : consultation et opérations sur ses comptes
- **Types de comptes** : Checking, Savings, Credit, Currency
- **Base de données embarquée H2** (mode Oracle, en mémoire) avec données de démonstration insérées au démarrage (`DemoSeeder`)
- **Utilitaires** : export PDF des relevés, journalisation, hachage des mots de passe, gestion de session

## Architecture

```
src/banksystem/
├── business/     # Classes métier (Account, Customer, Transaction, Validator…)
├── dao/          # Accès aux données (AccountDAO, CustomerDAO, LoginDAO, DBConnection…)
├── dto/          # Objets de transfert (AccountDTO, CustomerDTO, TransactionDTO…)
├── enums/        # Énumérations
├── exceptions/   # Exceptions personnalisées
├── service/     # Logique applicative (LoginService…)
├── ui/           # Interface Swing (Main, LoginWindow, BankManager, BankCustomer)
└── util/         # Utilitaires (BankLogger, PasswordUtil, PdfExporter, SessionManager)
```

## Prérequis

- **Java** 8 ou supérieur (JDK)
- Bibliothèque **H2** (dépendance JDBC) dans le classpath

## Compilation et exécution

Depuis la racine du projet :

```bash
# Compilation
javac -cp "lib/h2.jar" -d bin $(find src -name "*.java")

# Exécution
java -cp "bin:lib/h2.jar" banksystem.ui.Main
```

> Sous Windows, remplacez le séparateur `:` par `;` :
> `java -cp "bin;lib/h2.jar" banksystem.ui.Main`

La fenêtre de connexion s'ouvre ; le rôle de l'utilisateur (Client ou Gestionnaire) détermine la fenêtre affichée.

## Notes techniques

- La base H2 est créée **en mémoire** (`jdbc:h2:mem:bankdemo;MODE=Oracle`) : les données sont réinitialisées à chaque lancement et préremplies par `DemoSeeder`.
- Les mots de passe sont hachés via `PasswordUtil`.
- Les opérations sont journalisées via `BankLogger`.

## Auteur

**Francois-Xavier Dussault** — [github.com/francoisxavierdussault](https://github.com/francoisxavierdussault)
