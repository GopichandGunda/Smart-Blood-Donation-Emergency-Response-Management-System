# Emergency Blood Request & Donor Matching System

A Java 17 Swing desktop application for managing blood donors, emergency requests, compatible donor screening, blood-bank inventory, and blood transfers.

> **Clinical safety:** compatibility in this project is a preliminary red-cell screening aid, not a clinical decision. Qualified medical and blood-bank staff must confirm component type, ABO/Rh compatibility, crossmatch results, and local transfusion policy before blood is issued.

## Features

- Role-based sign-in for administrators, blood-bank staff, and donors.
- Donor self-registration, donor directory, availability, account status, and donation eligibility checks.
- Emergency request creation with critical/high/medium/low priority.
- Compatible donor screening with a transparent 100-point score and city preference.
- MySQL-backed requests, donor matches, and donor notification records.
- Blood stock entry, expiry handling, low/critical stock alerts, locked reservations, issue, and cancellation.
- Transactional donation recording with donor and inventory updates.
- Hospital and blood-bank directory management.
- Blood-bank transfer request, approval, dispatch, and receipt workflows.
- CSV/TXT summary report export and administrator audit/account screens.
- PBKDF2-HMAC-SHA-256 salted password hashes; database credentials are read from environment variables.
- JUnit 5 unit tests for compatibility, eligibility, matching, validation, inventory alerts, and password hashing.

## Technology

- Java 17+
- Java Swing, JDBC, Java Collections, Streams, lambdas, and `ExecutorService`
- MySQL 8.0.16+
- Maven
- JUnit 5

No web framework, application server, ORM, or browser UI is used.

## Architecture

```text
Swing UI (LoginFrame, DashboardFrame)
                 |
             Services
     eligibility / matching / inventory
                 |
           JDBC DAO layer
                 |
        MySQL (InnoDB tables)
```

Business rules live in services; SQL is parameterized with `PreparedStatement`. Reservation, donation, and transfer dispatch/receipt operations use transactions and row locks. Emergency searches use an `ExecutorService` so database work does not block Swing's event-dispatch thread.

### Project structure

```text
src/main/java/com/bloodmanagement/
  dao/        JDBC data access
  enums/      Shared roles, blood groups, and workflow states
  exception/  Domain and persistence exceptions
  model/      Domain records
  service/    Authentication, eligibility, matching, inventory, reporting
  ui/         Swing login and role dashboards
  util/       JDBC transactions, configuration, validation, password hashing
  Main.java   Desktop entry point
src/main/resources/
  database.sql
  sample-data.sql
src/test/java/  JUnit 5 tests
```

## Donor eligibility and matching

This educational workflow applies an 18-year minimum age, active account and donor availability checks, and a 90-day minimum interval since the last recorded donation. A blood-bank should adapt eligibility rules to applicable regulations and medical screening.

The project's red-cell ABO/Rh screening map is centralized in `BloodCompatibilityService`. Matching only considers active, available, eligible donors. The score is:

| Criterion | Points |
|---|---:|
| Blood group: exact / compatible | 40 / 30 |
| Eligibility | 20 |
| Availability | 15 |
| Same city | 15 |
| Donation-history interval | 5–10 |

Matches are sorted by score. Emergency requests are ordered critical-first. Neither ranking nor these educational rules authorize a transfusion.

## Setup

Requirements: JDK 17 and MySQL 8.0.16+. The included Maven Wrapper downloads the pinned Maven release when first used.

1. Create the schema and optional sample hospital/blood-bank stock:

   ```powershell
   Get-Content src\main\resources\database.sql | mysql -u root -p
   Get-Content src\main\resources\sample-data.sql | mysql -u root -p emergency_blood_management
   ```

2. Create a dedicated MySQL application user. Choose a strong local password and do not commit it:

   ```sql
   CREATE USER 'blood_app'@'localhost' IDENTIFIED BY 'choose-a-strong-local-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON emergency_blood_management.* TO 'blood_app'@'localhost';
   ```

3. Set connection variables in the PowerShell terminal from which the app will run:

   ```powershell
   $env:BLOOD_DB_URL = "jdbc:mysql://localhost:3306/emergency_blood_management?serverTimezone=UTC"
   $env:BLOOD_DB_USER = "blood_app"
   $env:BLOOD_DB_PASSWORD = Read-Host "Database password"
   ```

   `BLOOD_DB_URL` can be set to a TLS-enabled URL appropriate to your MySQL deployment. The default URL can be overridden with this variable.

4. Create the first administrator through the secure terminal bootstrap (password entry is not echoed):

   ```powershell
   .\mvnw.cmd exec:java "-Dexec.mainClass=com.bloodmanagement.util.BootstrapAdmin"
   ```

   Run it in a real terminal (not a terminal that lacks secure console input). Administrators can then create blood-bank staff accounts in the Accounts screen. Donors may register from the sign-in screen.

5. Build, test, and launch:

   ```powershell
   .\mvnw.cmd clean test package
   .\mvnw.cmd exec:java
   ```

In VS Code, open this folder, set the same environment variables in the integrated PowerShell terminal, then run the commands above. Java extensions can also launch `com.bloodmanagement.Main` with the configured environment.

## First run and sample data

The sample script creates one example hospital, one example bank, and a small sample stock lot. It does **not** create login accounts or donor records. Create the first admin with the bootstrap command; then add hospital/bank records as needed. `sample-data.sql` is idempotent for its example rows.

## Tests

```powershell
.\mvnw.cmd test
```

The unit tests do not need a live database. The MySQL-dependent application requires the schema and connection variables above.

## Security and privacy

- No default usernames/passwords or database credentials are committed.
- Passwords are hashed with PBKDF2-HMAC-SHA-256 and per-password random salts.
- Use a least-privileged MySQL user and protect patient/donor data in production.
- `.gitignore` excludes local IDE files, build output, logs, and `.env` files.
- This project is an engineering demonstration, not a certified clinical or blood-bank information system.

## Future production work

Before real-world use, add formal authorization policy enforcement and staff verification workflows, secure transport and key management, backups, data-retention controls, complete operational reporting, accessibility/usability testing, and independent clinical, security, and regulatory review.
