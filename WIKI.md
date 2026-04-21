# Rento Wiki

## Author

### Project Team
- **Hariharasudhan A** — sudanayyappan_bcs28@mepcoeng.ac.in
- **Hari Prasad V** — santhiselvan74_bcs28@mepcoeng.ac.in
- **Muhammed Yousuf M** — yousufilyas86bcs28@mepcoeng.ac.in

---

## Introduction

**Rento** is a JavaFX desktop application for end-to-end vehicle booking and supplier-driven rental operations.

It supports:
- Registration and login with CAPTCHA + BCrypt password hashing
- Role-based access (**USER**, **DRIVER**, **SUPPLIER**, **ADMIN**)
- Vehicle booking flow with dynamic pricing, tax, deposit, and OTP ride start verification
- Rental marketplace flow with supplier approval and OTP activation
- Payment simulation with multiple methods and validation
- Receipt/notification/export generation (TXT/PDF)

Primary entry point:
- `com.rento.app.RentoApplication`

Primary database:
- MongoDB at `mongodb://localhost:27017`
- DB name: `rento_db`

---

## Architecture

Rento follows a layered desktop architecture:

1. **Presentation Layer (JavaFX)**
   - FXML layouts + controller classes
   - Handles input, navigation, visual state, and feedback

2. **Application/Service Layer**
   - Business logic and workflow orchestration
   - Pricing, OTP flow, approval flow, notifications, receipts, exports

3. **Persistence Layer (DAO)**
   - MongoDB CRUD and mapping between model objects and BSON documents

4. **Domain Layer (Models)**
   - User, Vehicle, Booking, Rental, Payment entities + enums

5. **Infrastructure/Utilities**
   - MongoDB singleton connection
   - Validation, date-time utilities, session management, hashing, navigation

### Runtime Flow (high level)
- App starts (`RentoApplication`) → Mongo initializes → demo seed runs → landing page loads via `NavigationManager`
- User actions in controllers call services
- Services read/write Mongo using DAOs
- Results return to controllers for UI updates

### Key Service Responsibilities
- **AuthService**: registration/login/logout, validation integration, wallet initialization
- **BookingService**: booking creation, pricing, deposit, driver assignment, OTP ride verification, completion
- **RentalService**: supplier listing rental lifecycle (request → approve → OTP confirm → active/completed/overdue)
- **PaymentService**: payment method validation and simulated payment persistence
- **ReceiptService**: booking/rental receipt generation
- **NotificationService**: user notification persistence and export
- **AdminExportService**: full system snapshot export
- **DemoDataService**: initial users/fleet seeding

---

## Repository Structure

```text
Rento/
├── README.md
├── Documentation/
│   ├── SRS VRBS.pdf
│   └── SPECIFICATION VRBS.pdf
├── UML diagrams/
│   └── README.md
├── Source File/
│   ├── .classpath
│   ├── .project
│   ├── readme.md
│   ├── jars/                          # bundled JavaFX + external libs
│   └── rento/
│       ├── README.md
│       └── src/main/
│           ├── java/com/rento/
│           │   ├── app/               # application entry
│           │   ├── controllers/       # UI event handlers per screen
│           │   ├── services/          # business workflows
│           │   ├── dao/               # Mongo persistence
│           │   ├── models/            # domain entities
│           │   ├── navigation/        # scene navigation stack
│           │   ├── security/          # session + password hashing
│           │   └── utils/             # validation, datetime, alerts, mongodb
│           └── resources/
│               ├── css/
│               ├── fxml/
│               ├── fonts/
│               ├── images/
│               └── rsmd
└── .github/workflows/
    └── codeql.yml
```

---

## Component Details

### Controllers
- `LandingController`: guest/auth-aware entry navigation
- `LoginController`, `RegisterController`: CAPTCHA, credential handling, role route
- `BookingController`: browse/filter available vehicles
- `BookingDetailController`: pickup/drop config, pricing preview, booking creation
- `PaymentController`: payment validation, booking payment confirmation, OTP display
- `RentController`: supplier listing + renter marketplace flow
- `DriverDashboardController`: accept bookings, verify OTP, view metrics
- `SupplierDashboardController`: approve/reject rental requests, activate via OTP
- `AdminDashboardController`: platform metrics, vehicle approval, export, mail simulation
- `ProfileController`: user profile, stats, notifications, role dashboard routing
- `AboutController`, `ContactController`: static/nav pages

### DAOs and Mongo Collections
- `UserDAO` → `users`
- `VehicleDAO` → `vehicles`
- `BookingDAO` → `bookings`
- `RentalDAO` → `rentals`
- `PaymentDAO` → `payments`
- `NotificationService` (direct collection use) → `notifications`

### Core Business Rules
- Password must be 8+ chars with upper/lower/number/special
- Age allowed: 18–90
- Booking pricing includes:
  - 20% weekend surcharge
  - 10% discount for 7+ days
  - 18% tax
  - 25% deposit wallet debit at booking creation
- Driver earnings: 15% of completed booking total
- Rental overdue penalty: `overdueDays * pricePerDay * 0.25`
- Supplier listings require admin approval (`PENDING → APPROVED/REJECTED`)

---

## Valid Test Cases

### Authentication
1. Register with valid name/email/phone/age/password/captcha → success and redirected to login
2. Login with seeded user (`user@rento.local` / `Rento@123`) + correct captcha → routed to booking
3. Login as driver/supplier/admin with valid credentials → routed to corresponding dashboard

### Booking Flow
4. Logged-in user selects available vehicle, valid pickup/drop and time range, sufficient wallet → booking created
5. Driver accepts pending booking assigned to them → status becomes `CONFIRMED`
6. Driver enters correct OTP from customer payment screen → status becomes `IN_PROGRESS`
7. Driver completes booking → status `COMPLETED`, vehicle `UNDER_INSPECTION`, receipt notification generated

### Payment Flow
8. Card payment with valid card number (Luhn), CVV, expiry, holder name → payment recorded/simulated success
9. UPI payment with valid format (`name@bank`) + account holder → success
10. Net banking/wallet with non-empty valid reference + holder → success

### Rental Flow
11. Supplier submits new vehicle listing with valid values → stored with `PENDING` approval
12. Admin approves supplier listing → marketplace shows vehicle
13. User requests rental with valid period → rental status `REQUESTED`
14. Supplier approves request → rental `APPROVED` and OTP generated
15. Supplier confirms OTP correctly → rental `ACTIVE`, vehicle `IN_USE`
16. User marks active rental finished → rental `COMPLETED`, wallet transfer applied, vehicle `AVAILABLE`

### Admin/Profile/Export
17. Admin downloads full export → TXT file generated under user Documents folder
18. Profile notifications download with existing notifications → TXT file generated

---

## Invalid Test Cases

### Authentication/Input
1. Register with invalid email format → rejected
2. Register with weak password (e.g., no uppercase/special) → rejected
3. Register with age <18 or >90 → rejected
4. Register/login with wrong CAPTCHA answer → rejected and captcha regenerated
5. Login with non-existing email or wrong password → error shown

### Booking
6. Booking with return datetime <= pickup datetime → rejected
7. Booking for unavailable/maintenance vehicle → rejected
8. Booking when wallet < required deposit → rejected
9. Driver tries to accept booking reserved for another preferred driver → rejected
10. Driver OTP verification with wrong/non-6-digit OTP → rejected

### Payment
11. Credit/debit payment with invalid card number (fails Luhn) → rejected
12. Card payment with invalid expiry format/value → rejected
13. Card payment with invalid CVV → rejected
14. UPI format invalid (missing `@bank`) → rejected
15. Wallet/net-banking reference too short/empty → rejected

### Rental
16. Rental request without selected marketplace vehicle → blocked
17. Supplier account attempts to request rental as renter → blocked
18. Rental with invalid date range or non-positive hourly duration → rejected
19. Supplier OTP confirmation with wrong OTP → rejected
20. Attempting flow actions with guest session where auth is required → redirected to login

---

## Build, Run, and Validation Notes

- This repository does **not** include Maven/Gradle build or test definitions.
- Dependencies are bundled as JARs under `Source File/jars/` and configured via `.classpath` for IDE/manual execution.
- Main class to run: `com.rento.app.RentoApplication`.
- Automated test suites are not present; verification is primarily scenario/manual flow based.

---

## Wiki Usage Recommendation

For GitHub Wiki pages, this file can be used directly as:
- `Home.md` (main wiki landing page), or
- split into multiple wiki pages (Architecture, Test Cases, Modules, Setup) for easier navigation.
