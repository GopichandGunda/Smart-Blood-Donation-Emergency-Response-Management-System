# Emergency Blood Request & Donor Matching System

A Java 17 desktop application starter for coordinating emergency blood requests, donor information, and blood-bank operations. The project is being developed in phases; this repository currently contains **Phase 1 only**.

## Overview

The planned system will help authorized users manage donors, blood donations, emergency requests, compatible donor matching, and blood inventory through a Java Swing desktop application.

## Phase 1

This phase provides:

- A Maven project targeting Java 17.
- The package structure for the planned application layers.
- Core domain model records and shared enums.
- A runnable Swing welcome window.
- JUnit 5 configured for later phases.

Database access, authentication, operational workflows, matching, and the full role-based UI are not implemented yet.

## Technology

- Java 17
- Java Swing
- Maven
- JUnit 5

## Project Structure

```text
src/
  main/java/com/bloodmanagement/
    config/       Database configuration (future phase)
    dao/          Data access objects (future phase)
    enums/        Shared domain enums
    exception/    Application exceptions (future phase)
    model/        Core domain records
    service/      Business services (future phase)
    ui/           Swing application screens (future phase)
    util/         Shared utilities (future phase)
    Main.java     Swing application entry point
```

## Requirements

- JDK 17 or newer
- Apache Maven 3.8 or newer

Verify the tools are available:

```powershell
java -version
mvn -version
```

## Build and Run

From the project root:

```powershell
mvn clean package
mvn exec:java
```

The application opens a desktop window titled **Emergency Blood Request & Donor Matching System**.

In VS Code, open the project folder and run `com.bloodmanagement.Main` with the Java extension, or use the Maven commands above in the integrated terminal.

## Planned Development

The remaining phases will add MySQL/JDBC configuration, DAOs, services, donor and request workflows, conservative blood compatibility, inventory operations, Swing screens, reports, audit logging, and automated tests. Transfusion compatibility must always be confirmed by qualified medical or blood-bank staff.

## Configuration and Sensitive Data

Database credentials will be supplied through environment/configuration in the database phase. Do not commit passwords, credentials, logs containing sensitive information, or local environment files.
