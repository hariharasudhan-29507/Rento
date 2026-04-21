# Rento — Vehicle Rental & Booking System

Rento is a Java desktop application for end-to-end vehicle rental and booking management.  
It supports customer booking/rental flows and role-based operations for Admin, Supplier, and Driver users.

## What this project does

- User registration and login with secure password hashing
- Vehicle browsing, rental, and booking workflows
- Payment flow with receipt generation (PDF/TXT)
- Role-based dashboards:
  - **Admin**: platform-level operations and exports
  - **Supplier**: fleet and rental management
  - **Driver**: assigned booking management
- Demo data seeding for first-time local setup

## Tech Stack

- **Language:** Java
- **UI Framework:** JavaFX (FXML + CSS)
- **Database:** MongoDB
- **Database Driver:** MongoDB Java Driver (sync)
- **Security:** jBCrypt (password hashing), session handling
- **Reporting/Exports:** iText PDF, Gson, file-based exports
- **UI Add-ons:** ControlsFX
- **Logging:** SLF4J (simple backend)

## Project Structure

```text
Rento/
├── README.md
├── Documentation/
│   ├── SRS VRBS.pdf
│   └── SPECIFICATION VRBS.pdf
├── Source File/
│   ├── jars/                      # Third-party dependencies and JavaFX SDK
│   └── rento/
│       └── src/main/
│           ├── java/com/rento/
│           │   ├── app/           # App entry point
│           │   ├── controllers/   # JavaFX controllers
│           │   ├── services/      # Business logic
│           │   ├── dao/           # Data access (MongoDB)
│           │   ├── models/        # Domain models
│           │   ├── utils/         # Utilities and DB connection
│           │   ├── security/      # Auth/session helpers
│           │   └── navigation/    # Scene/navigation manager
│           └── resources/
│               ├── fxml/          # UI layouts
│               ├── css/           # Styling
│               └── images/        # App assets
└── UML diagrams/
    └── README.md
```

## Setup Guide (Complete)

### 1) Prerequisites

- JDK **21** (recommended)
- MongoDB Community Server (running locally on default port `27017`)
- IDE recommended: IntelliJ IDEA / Eclipse with JavaFX support

### 2) Clone and open the project

```bash
git clone https://github.com/hariharasudhan-29507/Rento.git
cd Rento
```

Open the source root:

- `Source File/rento/src/main/java`
- `Source File/rento/src/main/resources`

### 3) Configure dependencies

This project keeps JARs locally inside:

- `Source File/jars/`

Add all required JARs from that folder to your IDE project libraries, including:

- JavaFX JARs
- MongoDB driver JARs
- `jbcrypt-0.4.jar`
- `itextpdf-5.5.13.3.jar`
- `controlsfx-11.1.2.jar`
- `gson-2.10.1.jar`
- `slf4j-api` + `slf4j-simple`

### 4) Start MongoDB

Make sure MongoDB is running locally:

- Connection string used by the app: `mongodb://localhost:27017`
- Database name: `rento_db`

### 5) Run the application

Run the main class:

- `com.rento.app.RentoApplication`

On first launch, the app seeds demo users and vehicles if collections are empty.

## Demo Accounts (seeded automatically)

- `user@rento.local`
- `driver@rento.local`
- `supplier@rento.local`
- `admin@rento.local`

Default password for seeded users: `Rento@123`

## Wiki

- Project Wiki: https://github.com/hariharasudhan-29507/Rento/wiki

## Documentation

- [Software Requirements Specification](./Documentation/SRS%20VRBS.pdf)
- [ER/Project Specification](./Documentation/SPECIFICATION%20VRBS.pdf)
- [UML Diagrams Folder](./UML%20diagrams/README.md)

## Team

| Name | Email |
|------|-------|
| Hariharasudhan A | sudanayyappan_bcs28@mepcoeng.ac.in |
| Hari Prasad V | santhiselvan74_bcs28@mepcoeng.ac.in |
| Muhammed Yousuf M | yousufilyas86bcs28@mepcoeng.ac.in |
